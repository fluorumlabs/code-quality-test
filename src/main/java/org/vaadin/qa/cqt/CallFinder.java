package org.vaadin.qa.cqt;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by Artem Godin on 9/29/2020.
 */
public class CallFinder {
    private final Class<?> clazz;
    private final String fieldName;
    private final List<String> methods;

    public CallFinder(Member field, List<String> methods) {
        clazz = field.getDeclaringClass();
        fieldName = field.getName();
        this.methods = Collections.unmodifiableList(methods);
    }

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

    private boolean fieldWasConsumedFromStack(int index, Analyzer<SourceValue> analyzer) {
        Frame<SourceValue>[] frames = analyzer.getFrames();
        Frame<SourceValue> current = frames[index];
        Frame<SourceValue> next = index < frames.length-1 ? frames[index + 1] : current;

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
                    int indexOf = fields.indexOf(insnNode.name);
                    if (indexOf >= 0) {
                        fields.remove(indexOf);
                    }
                }
            }
        }

        return fields.contains(fieldName);
    }



    private Set<String> findCallingMethods() throws IOException, AnalyzerException {
        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(clazz.getName());
        cr.accept(classNode, 0);

        Map<String, Set<String>> selfReferences = new HashMap<>();
        Map<String, MethodNode> methodRefs = new HashMap<>();
        List<String> methodsWithInvocations = new ArrayList<>();

        for (MethodNode method : classNode.methods) {
            String signature = method.name + method.desc;
            methodRefs.put(method.name + method.desc, method);

            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            analyzer.analyze(classNode.name, method);

            AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray();
            for (int i = 0; i < abstractInsnNodes.length; i++) {
                if (abstractInsnNodes[i] instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) abstractInsnNodes[i];
                    if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL || insn.getOpcode() == Opcodes.INVOKEINTERFACE) {
                        if (fieldWasConsumedFromStack(i, analyzer) && methods.contains(insn.name)) {
                            methodsWithInvocations.add(signature);
                        }
                        if (insn.owner.equals(classNode.name)) {
                            selfReferences.computeIfAbsent(insn.name + insn.desc, s -> new HashSet<>()).add(signature);
                        }
                    } else if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        if (insn.owner.equals(classNode.name)) {
                            selfReferences.computeIfAbsent(insn.name + insn.desc, s -> new HashSet<>()).add(signature);
                        }
                    }
                } else if (abstractInsnNodes[i] instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) abstractInsnNodes[i];

                    boolean match = false;
                    boolean selfMatch = false;
                    String callChain = null;
                    for (Object bsmArg : insn.bsmArgs) {
                        if (bsmArg instanceof Handle) {
                            if (methods.contains(((Handle) bsmArg).getName())) {
                                match = true;
                            } else if (((Handle) bsmArg).getOwner().equals(classNode.name)) {
                                callChain = ((Handle) bsmArg).getName() + ((Handle) bsmArg).getDesc();
                                selfMatch = true;
                            }
                        }
                    }

                    if (fieldWasConsumedFromStack(i, analyzer) && match) {
                        methodsWithInvocations.add(signature);
                    }
                    if (selfMatch) {
                        selfReferences.computeIfAbsent(callChain, s -> new HashSet<>()).add(signature);
                    }
                }
            }
        }

        Set<String> result = new HashSet<>();
        Set<String> visitedMethods = new HashSet<>(methodsWithInvocations);
        Queue<String> unroll = new ArrayDeque<>(methodsWithInvocations);

        while (unroll.peek() != null) {
            String signature = unroll.poll();
            MethodNode methodNode = methodRefs.get(signature);
            if (!Modifier.isPrivate(methodNode.access) || methodNode.name.startsWith("<")) {
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
