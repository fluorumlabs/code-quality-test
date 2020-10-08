/*
 * Copyright (c) 2020 Artem Godin
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.fluorumlabs.cqt.internals;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.SourceInterpreter;
import jdk.internal.org.objectweb.asm.tree.analysis.SourceValue;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Helper to find methods that can update field.
 */
public class ModificationFinder {

    private final Class<?> clazz;

    private final String fieldName;

    /**
     * Instantiates a new modification finder.
     *
     * @param field the field
     */
    public ModificationFinder(Member field) {
        clazz     = field.getDeclaringClass();
        fieldName = field.getName();
    }

    /**
     * Test if the field is not modified by methods listed in argument
     *
     * @param methodNames the method names
     *
     * @return {@code true} if there are methods updating this field that are
     *         not listed in arguments
     */
    public boolean modifiedByNot(String... methodNames) {
        try {
            Set<String> modifyingMethods = findModifyingMethods();
            modifyingMethods.removeAll(Arrays.asList(methodNames));
            return !modifyingMethods.isEmpty();
        } catch (IOException | AnalyzerException e) {
            // ignore
        }
        return false;
    }

    private Set<String> findModifyingMethods() throws
                                               IOException,
                                               AnalyzerException {
        ClassNode   classNode = new ClassNode();
        ClassReader cr        = new ClassReader(clazz.getName());
        cr.accept(
                classNode,
                0
        );

        Map<String, Set<String>> selfReferences           = new HashMap<>();
        Map<String, MethodNode>  methodRefs               = new HashMap<>();
        List<String>             methodsWithModifications = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            String signature = method.name + method.desc;
            methodRefs.put(
                    method.name + method.desc,
                    method
            );

            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            analyzer.analyze(
                    classNode.name,
                    method
            );

            AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray();
            for (int i = 0; i < abstractInsnNodes.length; i++) {
                if (abstractInsnNodes[i] instanceof FieldInsnNode) {
                    FieldInsnNode insn = (FieldInsnNode) abstractInsnNodes[i];
                    if ((insn.getOpcode() == Opcodes.PUTFIELD
                         || insn.getOpcode() == Opcodes.PUTSTATIC)
                        && fieldName.equals(insn.name)) {
                        methodsWithModifications.add(signature);
                    }
                }
                if (abstractInsnNodes[i] instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) abstractInsnNodes[i];
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        || insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                        if (insn.owner.equals(classNode.name)) {
                            selfReferences.computeIfAbsent(
                                    insn.name + insn.desc,
                                    s -> new HashSet<>()
                            ).add(signature);
                        }
                    } else if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        if (insn.owner.equals(classNode.name)) {
                            selfReferences.computeIfAbsent(
                                    insn.name + insn.desc,
                                    s -> new HashSet<>()
                            ).add(signature);
                        }
                    }
                } else if (abstractInsnNodes[i] instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) abstractInsnNodes[i];

                    boolean selfMatch = false;
                    String  callChain = null;
                    for (Object bsmArg : insn.bsmArgs) {
                        if (bsmArg instanceof Handle) {
                            if (((Handle) bsmArg)
                                    .getOwner()
                                    .equals(classNode.name)) {
                                callChain = ((Handle) bsmArg).getName()
                                            + ((Handle) bsmArg).getDesc();
                                selfMatch = true;
                            }
                        }
                    }

                    if (selfMatch) {
                        selfReferences.computeIfAbsent(
                                callChain,
                                s -> new HashSet<>()
                        ).add(signature);
                    }
                }
            }
        }

        Set<String>   result         = new HashSet<>();
        Set<String>   visitedMethods = new HashSet<>(methodsWithModifications);
        Queue<String> unroll         = new ArrayDeque<>(methodsWithModifications);

        while (unroll.peek() != null) {
            String     signature  = unroll.poll();
            MethodNode methodNode = methodRefs.get(signature);
            if (!Modifier.isPrivate(methodNode.access)
                || methodNode.name.startsWith("<")) {
                result.add(methodNode.name);
            }
            if (selfReferences.get(signature) != null) {
                for (String s : selfReferences.get(signature)) {
                    if (visitedMethods.add(s)) {
                        unroll.add(s);
                    }
                }
            }
        }

        return result;
    }

}
