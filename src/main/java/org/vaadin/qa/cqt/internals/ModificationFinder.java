package org.vaadin.qa.cqt.internals;

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
     * @return {@code true} if there are methods updating this field that are not listed in arguments
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

    private final Class<?> clazz;

    private final String fieldName;

    private Set<String> findModifyingMethods() throws
                                               IOException,
                                               AnalyzerException {
        ClassNode   classNode = new ClassNode();
        ClassReader cr        = new ClassReader(clazz.getName());
        cr.accept(classNode, 0);

        Map<String, Set<String>> selfReferences           = new HashMap<>();
        Map<String, MethodNode>  methodRefs               = new HashMap<>();
        List<String>             methodsWithModifications = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            String signature = method.name + method.desc;
            methodRefs.put(method.name + method.desc, method);

            Analyzer<SourceValue> analyzer
                    = new Analyzer<>(new SourceInterpreter());
            analyzer.analyze(classNode.name, method);

            AbstractInsnNode[] abstractInsnNodes
                    = method.instructions.toArray();
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
                            selfReferences
                                    .computeIfAbsent(insn.name + insn.desc,
                                                     s -> new HashSet<>()
                                    )
                                    .add(signature);
                        }
                    } else if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        if (insn.owner.equals(classNode.name)) {
                            selfReferences
                                    .computeIfAbsent(insn.name + insn.desc,
                                                     s -> new HashSet<>()
                                    )
                                    .add(signature);
                        }
                    }
                } else if (abstractInsnNodes[i] instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode insn
                            = (InvokeDynamicInsnNode) abstractInsnNodes[i];

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
                        selfReferences
                                .computeIfAbsent(callChain,
                                                 s -> new HashSet<>()
                                )
                                .add(signature);
                    }
                }
            }
        }

        Set<String> result         = new HashSet<>();
        Set<String> visitedMethods = new HashSet<>(methodsWithModifications);
        Queue<String> unroll = new ArrayDeque<>(methodsWithModifications);

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
