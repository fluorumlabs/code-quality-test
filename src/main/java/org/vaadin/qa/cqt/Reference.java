package org.vaadin.qa.cqt;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public final class Reference {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(\\b[a-z0-9_]+[.]\\b)");
    private static final int MAX_CONTEXT_PATH_DEPTH = 5;

    @Nullable
    private final Object target;
    @Nullable
    private final Class<?> targetClass;
    @Nullable
    private final ObjectData targetData;

    private final Object owner;
    private final Class<?> ownerClass;
    private final ObjectData ownerData;

    private final ReferenceType referenceType;
    @Nullable
    private final Field field;

    private final Scanner scanner;

    private Reference(@Nullable Object target, @Nullable Class<?> targetClass, @Nullable ObjectData targetData, Object owner, Class<?> ownerClass, ObjectData ownerData, ReferenceType referenceType, @Nullable Field field, Scanner scanner) {
        this.target = target;
        this.targetClass = targetClass;
        this.targetData = targetData;
        this.owner = owner;
        this.ownerClass = ownerClass;
        this.ownerData = ownerData;
        this.referenceType = referenceType;
        this.field = field;
        this.scanner = scanner;
    }

    public static Reference from(Object owner, ObjectValue value, Scanner scanner) {
        Object targetValue = value.getValue();
        Class<?> targetClass = targetValue == null ? null : targetValue.getClass();
        ObjectData targetData = targetValue == null ? null : scanner.getData(targetValue);
        Class<?> ownerClass;
        if (value.getField() != null) {
            ownerClass = value.getField().getDeclaringClass();
        } else {
            ownerClass = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        }
        return new Reference(
                targetValue,
                targetClass,
                targetData,
                owner instanceof Class ? null : owner,
                ownerClass,
                scanner.getData(owner),
                value.getReferenceType(),
                value.getField(),
                scanner
        );
    }

    public static Reference from(Object owner, Scanner scanner) {
        Class<?> ownerClass;
        ownerClass = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        return new Reference(
                owner instanceof Class ? null : owner,
                ownerClass,
                scanner.getData(owner),
                owner instanceof Class ? null : owner,
                ownerClass,
                scanner.getData(owner),
                ReferenceType.ACTUAL_VALUE,
                null,
                scanner
        );
    }

    public static String formatClassName(Class<?> clazz) {
        if (clazz.isArray()) {
            return formatClassName(clazz.getComponentType()) + "[]";
        }

        StringBuilder sb = new StringBuilder(clazz.getCanonicalName() == null ? clazz.getName() : clazz.getCanonicalName());

        TypeVariable<?>[] typeparms = clazz.getTypeParameters();
        if (typeparms.length > 0) {
            boolean first = true;
            sb.append('<');
            for (TypeVariable<?> typeparm : typeparms) {
                if (!first)
                    sb.append(',');
                sb.append(PACKAGE_PATTERN.matcher(typeparm.getTypeName()).replaceAll(""));
                first = false;
            }
            sb.append('>');
        }

        return sb.toString();
    }

    public static String formatShortClassName(Class<?> clazz) {
        return PACKAGE_PATTERN.matcher(formatClassName(clazz)).replaceAll("");
    }

    public boolean isInScope() {
        return scanner.isInScope(ownerClass);
    }

    public String getScope() {
        return ownerData == null ? "instance" : ownerData.getScope();
    }

    public boolean hasOwnScope() {
        return ownerData!=null && ownerData.hasOwnScope();
    }

    public String formatScope() {
        return ownerData.getPrintableScope();
    }

    public String formatOwnerClass() {
        if (field == null) {
            return formatClassName(ownerClass);
        } else {
            return formatClassName(field.getDeclaringClass());
        }
    }

    public String formatOwnerClassWithLink() {
        String className = formatOwnerClass();

        String s = escapeHtml4(className);
        if (owner != null) {
            if (scanner.getData(owner) != null) {
                return "<a href='/" + encodeValue(Scanner.computeHash(owner)) + "'>" + s + "</a>";
            } else {
                return s;
            }
        } else {
            if (scanner.getData(ownerClass) != null) {
                return "<a href='/" + encodeValue(Scanner.computeHash(ownerClass)) + "'>" + s + "</a>";
            } else {
                return s;
            }
        }
    }

    public String formatField() {
        if (field == null) {
            return "";
        } else {
            return formatFieldModifiers() + " " + formatFieldType() + " " + field.getName() + referenceType;
        }
    }

    public String formatFieldModifiers() {
        if (field == null) {
            return "";
        }
        return Modifier.toString(field.getModifiers());
    }

    public String formatFieldType() {
        if (field == null) {
            return "";
        }
        return PACKAGE_PATTERN.matcher(field.getGenericType().getTypeName()).replaceAll("");
    }

    public String formatReference() {
        if (field == null) {
            return formatClassName(ownerClass);
        } else {
            if (owner != null && !field.getDeclaringClass().getName().equals(owner.getClass().getName())) {
                return formatClassName(owner.getClass()) + ": " + formatClassName(field.getDeclaringClass()) + ": " + formatFieldModifiers() + " " + formatFieldType() + " " + field.getName() + referenceType;
            } else {
                return formatClassName(field.getDeclaringClass()) + ": " + formatFieldModifiers() + " " + formatFieldType() + " " + field.getName() + referenceType;
            }
        }
    }

    public String formatPartial() {
        if (field == null) {
            return "";
        } else {
            return "." + (Modifier.isStatic(field.getModifiers()) ? "(static)" : "") + field.getName() + referenceType;
        }
    }

    public List<String> formatBackreferences() {
        if (target == null) {
            return Collections.emptyList();
        }
        Map<Reference, String> map = new HashMap<>();

        String formattedField = formatField();

        for (Reference backreference : scanner.getBackreferences(target)) {
            if (backreference.owner != owner || !backreference.formatField().equals(formattedField)) {
                map.put(backreference, backreference.formatPartial());
            }
        }

        return map.entrySet().stream()
                .filter(ref -> ref.getKey().isInScope())
                .map(e -> e.getKey().formatOwnerClassWithLink() + escapeHtml4(e.getValue()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public String formatPathToContext() {
        if (owner == null || getScope().equals("instance") || hasOwnScope()) {
            return "";
        }
        Map<Reference, String> map = new HashMap<>();
        Map<Object, Boolean> visitedObjects = new IdentityHashMap<>();
        map.put(this, "");

        for (int i = 0; i < MAX_CONTEXT_PATH_DEPTH; i++) {
            List<Reference> refList = new ArrayList<>(map.keySet());
            for (Reference reference : refList) {
                if (visitedObjects.containsKey(reference.owner)) {
                    continue;
                }
                visitedObjects.put(reference.owner, true);
                for (Reference backreference : scanner.getBackreferences(reference.owner)) {
                    if(backreference.hasOwnScope() && backreference.getScope().equals(getScope())) {
                        return formatClassName(backreference.ownerClass)+backreference.formatPartial()+map.get(reference);
                    }
                    map.put(backreference, backreference.formatPartial() + map.get(reference));
                }
            }
        }
        return "";
    }

    public String formatValue() {
        if (target == null) {
            return "null";
        }
        String formatted;
        if (target instanceof String) {
            formatted = "\"" + escapeJava((String) target) + "\"";
        } else if (Modifier.isFinal(targetClass.getModifiers()) && (target instanceof Number || target instanceof Character || target instanceof Boolean)) {
            formatted = Objects.toString(target);
        } else {
            try {
                if (targetClass.isArray()) {
                    formatted = "(" + formatShortClassName(targetClass) + ") " + Arrays.toString((Object[]) target);
                } else {
                    formatted = "(" + formatShortClassName(targetClass) + ") " + Objects.toString(target);
                }
            } catch (Exception e) {
                // ignore exceptions during toString
                formatted = formatClassName(targetClass) + "@" + Integer.toHexString(System.identityHashCode(target));
            }
        }

        String s = escapeHtml4(trimTo(formatted, 120));
        if (scanner.getData(target)!=null) {
            return "<a href='/"+encodeValue(Scanner.computeHash(target))+"'>" + s + "</a>";
        } else {
            return s;
        }
    }

    @Nullable
    public Object getTarget() {
        return target;
    }

    @Nullable
    public Class<?> getTargetClass() {
        return targetClass;
    }

    @Nullable
    public ObjectData getTargetData() {
        return targetData;
    }

    public Object getOwner() {
        return owner;
    }

    public Class<?> getOwnerClass() {
        return ownerClass;
    }

    public ObjectData getOwnerData() {
        return ownerData;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    @Nullable
    public Field getField() {
        return field;
    }

    public Scanner getScanner() {
        return scanner;
    }

    private static String trimTo(String text, int length) {
        text = text.replace('\n', ' ').replace('\r', ' ');
        if (text.length() < length) {
            return text;
        } else {
            return text.substring(0, length)+"...";
        }
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
}
