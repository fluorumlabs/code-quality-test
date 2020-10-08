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

package org.vaadin.qa.cqt.internals;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;

import java.io.IOException;
import java.lang.reflect.Member;
import java.util.*;

/**
 * Helper to find visible/exposed methods which call specific methods of an
 * object that can be stored in a specific field.
 */
public class CallFinder {

    private final Class<?> clazz;

    private final String fieldName;

    private final List<String> methods;

    /**
     * Instantiates a new call finder for declaring class of field.
     *
     * @param field   the field
     * @param methods the list of methods
     */
    public CallFinder(Member field, List<String> methods) {
        clazz        = field.getDeclaringClass();
        fieldName    = field.getName();
        this.methods = Collections.unmodifiableList(methods);
    }

    /**
     * Test if methods specified in {@link CallFinder#CallFinder(Member, List)}
     * are not called from listed methods.
     *
     * @param methodNames the method names
     *
     * @return the boolean
     */
    public boolean calledByNot(String... methodNames) {
        try {
            Set<String> callingMethods = findCallingMethods();
            callingMethods.removeAll(Arrays.asList(methodNames));
            return !callingMethods.isEmpty();
        } catch (IOException | AnalyzerException e) {
            // ignore
        }
        return false;
    }

    private Set<String> findCallingMethods() throws
                                             IOException,
                                             AnalyzerException {
        ClassNode   classNode = new ClassNode();
        ClassReader cr        = new ClassReader(clazz.getName());
        cr.accept(
                classNode,
                0
        );

        Map<String, Set<String>> selfReferences         = new HashMap<>();
        Map<String, MethodNode>  methodRefs             = new HashMap<>();
        List<String>             methodsWithInvocations = new ArrayList<>();

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
                if (abstractInsnNodes[i] instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) abstractInsnNodes[i];
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        || insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                        if (fieldWasConsumedFromStack(
                                i,
                                analyzer
                        ) && methods.contains(insn.name)) {
                            methodsWithInvocations.add(signature);
                        }
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

                    boolean match     = false;
                    boolean selfMatch = false;
                    String  callChain = null;
                    for (Object bsmArg : insn.bsmArgs) {
                        if (bsmArg instanceof Handle) {
                            if (methods.contains(((Handle) bsmArg).getName())) {
                                match = true;
                            } else if (((Handle) bsmArg)
                                    .getOwner()
                                    .equals(classNode.name)) {
                                callChain = ((Handle) bsmArg).getName()
                                            + ((Handle) bsmArg).getDesc();
                                selfMatch = true;
                            }
                        }
                    }

                    if (fieldWasConsumedFromStack(
                            i,
                            analyzer
                    ) && match) {
                        methodsWithInvocations.add(signature);
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
        Set<String>   visitedMethods = new HashSet<>(methodsWithInvocations);
        Queue<String> unroll         = new ArrayDeque<>(methodsWithInvocations);

        while (unroll.peek() != null) {
            String     signature  = unroll.poll();
            MethodNode methodNode = methodRefs.get(signature);
            if (ExposedMembers.isMethodExposed(
                    classNode.name,
                    methodNode.name,
                    methodNode.desc
            ) || methodNode.name.startsWith("<")) {
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

    private boolean fieldWasConsumedFromStack(int index,
                                              Analyzer<SourceValue> analyzer) {
        Frame<SourceValue>[] frames  = analyzer.getFrames();
        Frame<SourceValue>   current = frames[index];
        Frame<SourceValue> next = index < frames.length - 1
                                  ? frames[index + 1]
                                  : current;

        List<String> fields = new ArrayList<>();
        for (int i = 0; i < current.getStackSize(); i++) {
            SourceValue instance = current.getStack(i);
            for (AbstractInsnNode abstractInsnNode : instance.insns) {
                if (abstractInsnNode instanceof FieldInsnNode) {
                    FieldInsnNode insnNode = (FieldInsnNode) abstractInsnNode;
                    fields.add(insnNode.name);
                }
            }
        }

        for (int i = 0; i < next.getStackSize(); i++) {
            SourceValue instance = next.getStack(i);
            for (AbstractInsnNode abstractInsnNode : instance.insns) {
                if (abstractInsnNode instanceof FieldInsnNode) {
                    FieldInsnNode insnNode = (FieldInsnNode) abstractInsnNode;
                    int           indexOf  = fields.indexOf(insnNode.name);
                    if (indexOf >= 0) {
                        fields.remove(indexOf);
                    }
                }
            }
        }

        return fields.contains(fieldName);
    }

}
