package org.vaadin.qa.cqt;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;
import org.vaadin.qa.cqt.suites.Classes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.internal.org.objectweb.asm.Type.*;

/**
 * Created by Artem Godin on 9/29/2020.
 */
public class PossibleValues {
    private final Class<?> clazz;

    public PossibleValues(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Map<String, Set<PossibleValue>> findPossibleValues() {
        try {
            return collectPossibleValues();
        } catch (IOException | AnalyzerException e) {
            // ignore
        }
        return Collections.emptyMap();
    }

    private Map<String, Set<PossibleValue>> collectPossibleValues() throws IOException, AnalyzerException {
        Map<String, Map<String, PossibleValue>> results = new HashMap<>();

        ClassNode classNode = new ClassNode();
        ClassReader cr = new ClassReader(clazz.getName());
        cr.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            analyzer.analyze(classNode.name, method);

            AbstractInsnNode[] abstractInsnNodes = method.instructions.toArray();
            for (int i = 0; i < abstractInsnNodes.length; i++) {
                if (abstractInsnNodes[i] instanceof FieldInsnNode) {
                    FieldInsnNode insn = (FieldInsnNode) abstractInsnNodes[i];
                    if ((insn.getOpcode() == Opcodes.PUTFIELD || insn.getOpcode() == Opcodes.PUTSTATIC)) {
                        String fieldName = insn.name;
                        String fieldDesc = Type.getType(insn.desc).getClassName();
                        boolean array = insn.desc.startsWith("[");
                        Frame<SourceValue>[] frames = analyzer.getFrames();
                        Frame<SourceValue> current = frames[i];
                        SourceValue topValue = current.getStack(current.getStackSize() - 1);
                        for (AbstractInsnNode abstractInsnNode : topValue.insns) {
                            if (abstractInsnNode instanceof TypeInsnNode) {
                                String desc = Type.getObjectType(((TypeInsnNode) abstractInsnNode).desc).getClassName();
                                addPossibleFieldValue(results, fieldName, fieldDesc, array, desc, method);
                            } else if (abstractInsnNode instanceof MethodInsnNode) {
                                String desc = Type.getType(((MethodInsnNode) abstractInsnNode).desc).getReturnType().getClassName();
                                String name = ((MethodInsnNode) abstractInsnNode).name;
                                if (((MethodInsnNode) abstractInsnNode).owner.equals(Type.getInternalName(Collections.class))) {
                                    desc = getCollectionsWrapperClass(name, desc);
                                }
                                addPossibleFieldValue(results, fieldName, fieldDesc, array, desc, method);
                            }
                        }
                    }
                }
            }
        }

        Map<String, Set<PossibleValue>> processedResults = new HashMap<>();
        for (Map.Entry<String, Map<String, PossibleValue>> entry : results.entrySet()) {
            processedResults.put(entry.getKey(), new HashSet<>(entry.getValue().values()));
        }

        return processedResults;
    }

    private void addPossibleFieldValue(Map<String, Map<String, PossibleValue>> results, String fieldName, String fieldDesc, boolean array, String desc, MethodNode method) {
        if (desc.endsWith("[]") || desc.indexOf('.') < 0) {
            // Skip arrays or primitives
            return;
        }
        try {
            Class<?> potentialClass = Thread.currentThread().getContextClassLoader().loadClass(desc);
            String methodName = formatMethodName(method);

            if (array) {
                Class<?> arrayClass = Array.newInstance(potentialClass, 0).getClass();
                results.computeIfAbsent(fieldName, fn -> new HashMap<>())
                        .computeIfAbsent(arrayClass.getName(), cn -> new PossibleValue(arrayClass, clazz))
                        .addMethod(methodName);
            } else {
                results.computeIfAbsent(fieldName, fn -> new HashMap<>())
                        .computeIfAbsent(potentialClass.getName(), cn -> new PossibleValue(potentialClass, clazz))
                        .addMethod(methodName);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    private static String getClassName(Type type) {
        switch (type.getSort()) {
            case VOID:
                return "void";
            case BOOLEAN:
                return "boolean";
            case CHAR:
                return "char";
            case BYTE:
                return "byte";
            case SHORT:
                return "short";
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case LONG:
                return "long";
            case DOUBLE:
                return "double";
            case ARRAY:
                StringBuilder stringBuilder = new StringBuilder(getClassName(type.getElementType()));
                for (int i = type.getDimensions(); i > 0; --i) {
                    stringBuilder.append("[]");
                }
                return stringBuilder.toString();
            case OBJECT:
            default:
                return "{" + type.getInternalName().substring(type.getInternalName().lastIndexOf('/')+1) + "}";
        }
    }


    private String formatMethodName(MethodNode methodNode) {
        return methodNode.name + "(" +
                Stream.of(getArgumentTypes(methodNode.desc))
                        .map(PossibleValues::getClassName)
                        .collect(Collectors.joining(", "))
                + ")";
    }

    private String getCollectionsWrapperClass(String name, String defaultValue) {
        switch (name) {
            case "emptyList":
                return Classes.EMPTY_LIST.getName();
            case "emptyMap":
                return Classes.EMPTY_MAP.getName();
            case "emptySortedMap":
                return Classes.EMPTY_SORTED_MAP.getName();
            case "emptyNavigableMap":
                return Classes.EMPTY_NAVIGABLE_MAP.getName();
            case "emptySet":
                return Classes.EMPTY_SET.getName();
            case "emptySortedSet":
                return Classes.EMPTY_SORTED_SET.getName();
            case "emptyNavigableSet":
                return Classes.EMPTY_NAVIGABLE_SET.getName();

            case "unmodifiableCollection":
                return Classes.UNMODIFIABLE_COLLECTION.getName();
            case "unmodifiableList":
                return Classes.UNMODIFIABLE_LIST.getName();
            case "unmodifiableMap":
                return Classes.UNMODIFIABLE_MAP.getName();
            case "unmodifiableSortedMap":
                return Classes.UNMODIFIABLE_SORTED_MAP.getName();
            case "unmodifiableNavigableMap":
                return Classes.UNMODIFIABLE_NAVIGABLE_MAP.getName();
            case "unmodifiableSet":
                return Classes.UNMODIFIABLE_SET.getName();
            case "unmodifiableSortedSet":
                return Classes.UNMODIFIABLE_SORTED_SET.getName();
            case "unmodifiableNavigableSet":
                return Classes.UNMODIFIABLE_NAVIGABLE_SET.getName();

            case "synchronizedList":
                return Classes.SYNCHRONIZED_LIST.getName();
            case "synchronizedMap":
                return Classes.SYNCHRONIZED_MAP.getName();
            case "synchronizedSortedMap":
                return Classes.SYNCHRONIZED_SORTED_MAP.getName();
            case "synchronizedNavigableMap":
                return Classes.SYNCHRONIZED_NAVIGABLE_MAP.getName();
            case "synchronizedSet":
                return Classes.SYNCHRONIZED_SET.getName();
            case "synchronizedSortedSet":
                return Classes.SYNCHRONIZED_SORTED_SET.getName();
            case "synchronizedNavigableSet":
                return Classes.SYNCHRONIZED_NAVIGABLE_SET.getName();

            case "singleton":
                return Classes.SINGLETON.getName();
            case "singletonList":
                return Classes.SINGLETON_LIST.getName();
            case "singletonMap":
                return Classes.SINGLETON_MAP.getName();
        }
        return defaultValue;
    }

}