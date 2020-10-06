package org.vaadin.qa.cqt;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;
import org.vaadin.qa.cqt.suites.Classes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Artem Godin on 9/29/2020.
 */
public class ExposedMembers {
    private static final Set<String> EXPOSED_READING_FIELDS = new HashSet<>();
    private static final Set<String> EXPOSED_WRITING_FIELDS = new HashSet<>();
    private static final Set<String> EXPOSED_METHODS = new HashSet<>();

    private final Class<?> clazz;

    public ExposedMembers(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void collect() {
        try {
            collectExposedMembers();
        } catch (IOException | AnalyzerException e) {
            // ignore
        }
    }

    private void collectExposedMembers() throws IOException, AnalyzerException {
        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(clazz.getName());
        cr.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray();
            for (int i = 0; i < abstractInsnNodes.length; i++) {
                if (abstractInsnNodes[i] instanceof FieldInsnNode) {
                    FieldInsnNode insn = (FieldInsnNode) abstractInsnNodes[i];
                    if (!insn.owner.equals(classNode.name)) {
                        if (insn.getOpcode()== Opcodes.PUTFIELD || insn.getOpcode()==Opcodes.PUTSTATIC) {
                            EXPOSED_WRITING_FIELDS.add(insn.owner + "." + insn.name);
                        } else if (insn.getOpcode()== Opcodes.GETFIELD || insn.getOpcode()==Opcodes.GETSTATIC) {
                            EXPOSED_READING_FIELDS.add(insn.owner + "." + insn.name);
                        }
                    }
                } else if (abstractInsnNodes[i] instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) abstractInsnNodes[i];
                    if (!insn.owner.equals(classNode.name)) {
                        EXPOSED_METHODS.add(insn.owner+"."+insn.name+insn.desc);
                    }
                }
            }
        }
    }

    public static boolean isFieldExposedForReading(Field field) {
        return EXPOSED_READING_FIELDS.contains(Type.getInternalName(field.getDeclaringClass()) + "." + field.getName());
    }

    public static boolean isFieldExposedForReading(String owner, String field) {
        return EXPOSED_READING_FIELDS.contains(owner + "." + field);
    }

    public static boolean isFieldExposedForWriting(Field field) {
        return EXPOSED_WRITING_FIELDS.contains(Type.getInternalName(field.getDeclaringClass()) + "." + field.getName());
    }

    public static boolean isFieldExposedForWriting(String owner, String field) {
        return EXPOSED_WRITING_FIELDS.contains(owner + "." + field);
    }

    public static boolean isMethodExposed(Method method) {
        return EXPOSED_METHODS.contains(Type.getInternalName(method.getDeclaringClass()) + "." + method.getName() + Type.getMethodDescriptor(method));
    }

    public static boolean isMethodExposed(String owner, String method, String desc) {
        return EXPOSED_METHODS.contains(owner + "." + method + desc);
    }

}
