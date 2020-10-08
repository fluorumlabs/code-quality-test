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
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper to find all classes referensing fields or methods.
 */
public class ExposedMembers {

    private static final Set<String> EXPOSED_METHODS = new HashSet<>();

    private static final Set<String> EXPOSED_READING_FIELDS = new HashSet<>();

    private static final Set<String> EXPOSED_WRITING_FIELDS = new HashSet<>();

    private final Class<?> clazz;

    /**
     * Instantiates a new helper.
     *
     * @param clazz the class to scan
     */
    public ExposedMembers(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Test if field is read from other classes.
     *
     * @param field the field
     *
     * @return {@code true} if there are other classes that can read that
     *         particular field
     */
    public static boolean isFieldExposedForReading(Field field) {
        return EXPOSED_READING_FIELDS.contains(Type.getInternalName(field.getDeclaringClass())
                                               + "."
                                               + field.getName());
    }

    /**
     * Test if field is updated from other classes.
     *
     * @param field the field
     *
     * @return {@code true} if there are other classes that can update that
     *         particular field
     */
    public static boolean isFieldExposedForWriting(Field field) {
        return EXPOSED_WRITING_FIELDS.contains(Type.getInternalName(field.getDeclaringClass())
                                               + "."
                                               + field.getName());
    }

    /**
     * Test if method is called from other classes.
     *
     * @param method the method
     *
     * @return {@code true} if there are other classes that can call that
     *         particular method
     */
    public static boolean isMethodExposed(Method method) {
        return EXPOSED_METHODS.contains(Type.getInternalName(method.getDeclaringClass())
                                        + "."
                                        + method.getName()
                                        + Type.getMethodDescriptor(method));
    }

    /**
     * Test if field is read from other classes.
     *
     * @param owner the ASM internal name for class
     * @param field the field name
     *
     * @return {@code true} if there are other classes that can read that
     *         particular field
     */
    static boolean isFieldExposedForReading(String owner, String field) {
        return EXPOSED_READING_FIELDS.contains(owner + "." + field);
    }

    /**
     * Test if field is updated from other classes.
     *
     * @param owner the ASM internal name for class
     * @param field the field name
     *
     * @return {@code true} if there are other classes that can update that
     *         particular field
     */
    static boolean isFieldExposedForWriting(String owner, String field) {
        return EXPOSED_WRITING_FIELDS.contains(owner + "." + field);
    }

    /**
     * Test if method is called from other classes.
     *
     * @param owner  the ASM internal name for class
     * @param method the method name
     * @param desc   the ASM method descriptor
     *
     * @return {@code true} if there are other classes that can call that
     *         particular method
     */
    static boolean isMethodExposed(String owner, String method, String desc) {
        return EXPOSED_METHODS.contains(owner + "." + method + desc);
    }

    /**
     * Collect all fields/methods referenced from specified class.
     */
    public void collect() {
        try {
            collectExposedMembers();
        } catch (IOException e) {
            // ignore
        }
    }

    private void collectExposedMembers() throws IOException {
        ClassNode   classNode = new ClassNode();
        ClassReader cr        = new ClassReader(clazz.getName());
        cr.accept(
                classNode,
                0
        );

        for (MethodNode method : classNode.methods) {
            AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray();
            for (int i = 0; i < abstractInsnNodes.length; i++) {
                if (abstractInsnNodes[i] instanceof FieldInsnNode) {
                    FieldInsnNode insn = (FieldInsnNode) abstractInsnNodes[i];
                    if (!insn.owner.equals(classNode.name)) {
                        if (insn.getOpcode() == Opcodes.PUTFIELD
                            || insn.getOpcode() == Opcodes.PUTSTATIC) {
                            EXPOSED_WRITING_FIELDS.add(insn.owner
                                                       + "."
                                                       + insn.name);
                        } else if (insn.getOpcode() == Opcodes.GETFIELD
                                   || insn.getOpcode() == Opcodes.GETSTATIC) {
                            EXPOSED_READING_FIELDS.add(insn.owner
                                                       + "."
                                                       + insn.name);
                        }
                    }
                } else if (abstractInsnNodes[i] instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) abstractInsnNodes[i];
                    if (!insn.owner.equals(classNode.name)) {
                        EXPOSED_METHODS.add(insn.owner
                                            + "."
                                            + insn.name
                                            + insn.desc);
                    }
                }
            }
        }
    }

}
