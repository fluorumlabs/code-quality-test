package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.utils.HtmlFormatter;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.vaadin.qa.cqt.utils.HtmlFormatter.value;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public final class Reference {
    private static final int MAX_CONTEXT_PATH_DEPTH = 10;

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("(,\\s*)");
    private static final Pattern LT_PATTERN = Pattern.compile("&lt;", Pattern.LITERAL);
    private static final Pattern GT_PATTERN = Pattern.compile("&gt;", Pattern.LITERAL);
    private static final Pattern EMPTY_CLASS_PATTERN = Pattern.compile("<span class=\"class\"></span>", Pattern.LITERAL);
    private static final Pattern INNER_CLASS_PATTERN = Pattern.compile("(\\b([A-Za-z0-9][A-Za-z0-9_]*)[$]\\b)");

    private static final HtmlFormatter SCOPE_FORMAT = value().escapeHtml().styled("scope");
    private static final HtmlFormatter PACKAGE_NAME_FORMAT = value().escapeHtml().styled("class package");
    private static final HtmlFormatter CLASS_NAME_FORMAT = value().escapeHtml().styled("class");
    private static final HtmlFormatter TYPE_NAME_FORMAT = value()
            .removePackages()
            .escapeHtml()
            .replace(INNER_CLASS_PATTERN, "$2.")
            .replace(LT_PATTERN, "</span><span class=\"generic\">&lt;<span class=\"class\">")
            .replace(GT_PATTERN, "</span>&gt;</span><span class=\"class\">")
            .replace(SEPARATOR_PATTERN, "</span>$1<span class=\"class\">")
            .styled("class")
            .replace(EMPTY_CLASS_PATTERN, "");
    private static final HtmlFormatter GENERIC_ARGUMENTS_FORMAT = value().styled("generic");
    private static final HtmlFormatter REFERENCE_TYPE_FORMAT = value().escapeHtml().styled("reference");
    private static final HtmlFormatter VISIBILITY_MODIFIER_FORMAT = value().styled("modifier");
    private static final HtmlFormatter FIELD_NAME_FORMAT = value().escapeHtml().styled("field");
    private static final HtmlFormatter PARTIAL_FORMAT = value().styled("partial");
    private static final HtmlFormatter STATIC_FORMAT = value().styled("static");
    private static final HtmlFormatter TYPEHINT_VALUE_FORMAT = value().styled("typehint");
    private static final HtmlFormatter NULL_VALUE_FORMAT = value().styled("value null");
    private static final HtmlFormatter STRING_VALUE_FORMAT = value().removeNewLines().escapeJava().wrapWith("\"").trimTo(240).escapeHtml().styled("value string");
    private static final HtmlFormatter PRIMITIVE_VALUE_FORMAT = value().escapeHtml().styled("value primitive");
    private static final HtmlFormatter TOSTRING_VALUE_FORMAT = value().removeNewLines().trimTo(240).escapeHtml().styled("value");
    private static final HtmlFormatter DEFAULT_VALUE_FORMAT = value().escapeHtml().styled("value default");
    @Nullable
    private final Object target;
    @Nullable
    private final Class<?> targetClass;
    private final Object owner;
    private final Class<?> ownerClass;
    private final ObjectData ownerData;
    private final ReferenceType referenceType;
    @Nullable
    private final Field field;
    private final Scanner scanner;

    private Reference(@Nullable Object target, @Nullable Class<?> targetClass, Object owner, Class<?> ownerClass, ObjectData ownerData, ReferenceType referenceType, @Nullable Field field, Scanner scanner) {
        this.target = target;
        this.targetClass = targetClass;
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
        Class<?> ownerClass;
        if (value.getField() != null) {
            ownerClass = value.getField().getDeclaringClass();
        } else {
            ownerClass = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        }
        return new Reference(
                targetValue,
                targetClass,
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
                owner instanceof Class ? null : owner,
                ownerClass,
                scanner.getData(owner),
                ReferenceType.ACTUAL_VALUE,
                null,
                scanner
        );
    }

    public static String formatClassName(Class<?> clazz) {
        return formatClassName(clazz, true);
    }

    public static String formatClassName(Class<?> clazz, boolean includePackage) {
        if (clazz.isArray()) {
            return formatClassName(clazz.getComponentType(), includePackage) + REFERENCE_TYPE_FORMAT.format("[]");
        }

        LinkedList<String> classParts = new LinkedList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            classParts.addFirst(currentClass.getSimpleName());
            currentClass = currentClass.getDeclaringClass();
        }

        String className;
        if (includePackage) {
            className = (clazz.getPackage() == null ? "" : PACKAGE_NAME_FORMAT.format(clazz.getPackage().getName() + ".")) + CLASS_NAME_FORMAT.format(String.join(".", classParts));
        } else {
            className = CLASS_NAME_FORMAT.format(String.join(".", classParts));
        }

        StringBuilder sb = new StringBuilder("");

        TypeVariable<?>[] typeparms = clazz.getTypeParameters();
        if (typeparms.length > 0) {
            boolean continued = false;
            sb.append('<');
            for (TypeVariable<?> typeparm : typeparms) {
                if (continued)
                    sb.append(',');
                sb.append(TYPE_NAME_FORMAT.format(typeparm.getTypeName()));
                continued = true;
            }
            sb.append('>');
        }

        return className + GENERIC_ARGUMENTS_FORMAT.format(sb);
    }

    public static String formatShortClassName(Class<?> clazz) {
        return formatClassName(clazz, false);
    }

    public boolean isInScope() {
        return scanner.isInScope(ownerClass);
    }

    public String getScope() {
        return ownerData == null ? "instance" : ownerData.getScope();
    }

    public boolean hasOwnScope() {
        return ownerData != null && ownerData.hasOwnScope();
    }

    public String formatScope() {
        return SCOPE_FORMAT.format(ownerData.getPrintableScope());
    }

    public String formatOwnerClass() {
        if (field == null) {
            return formatClassName(ownerClass);
        } else {
            return formatClassName(field.getDeclaringClass());
        }
    }

    public String formatField() {
        if (field == null) {
            return "";
        } else {
            return VISIBILITY_MODIFIER_FORMAT.format(getFieldModifiers()) + " "
                    + TYPE_NAME_FORMAT.format(getFieldType()) + " "
                    + FIELD_NAME_FORMAT.format(field.getName())
                    + REFERENCE_TYPE_FORMAT.format(referenceType);
        }
    }

    public String getFieldModifiers() {
        if (field == null) {
            return "";
        }
        return Modifier.toString(field.getModifiers());
    }

    public String getFieldType() {
        if (field == null) {
            return "";
        }
        return field.getGenericType().getTypeName();
    }

    public String getReference() {
        if (field == null) {
            return ownerClass.getTypeName();
        } else {
            if (owner != null && !field.getDeclaringClass().getName().equals(owner.getClass().getName())) {
                return owner.getClass().getTypeName() + ": " + field.getDeclaringClass().getTypeName() + ": " + getFieldModifiers() + " " + getFieldType() + " " + field.getName() + referenceType;
            } else {
                return field.getDeclaringClass().getTypeName() + ": " + getFieldModifiers() + " " + getFieldType() + " " + field.getName() + referenceType;
            }
        }
    }

    public String formatPartial() {
        if (field == null) {
            return "";
        } else {
            String partial = FIELD_NAME_FORMAT.format(field.getName()) + REFERENCE_TYPE_FORMAT.format(referenceType);
            if (Modifier.isStatic(field.getModifiers())) {
                return "." + STATIC_FORMAT.format(partial);
            } else {
                return "." + partial;
            }
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
                .map(e -> e.getKey().formatOwnerClass() + PARTIAL_FORMAT.format(e.getValue()))
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
                    if (backreference.hasOwnScope() && backreference.getScope().equals(getScope())) {
                        return formatClassName(backreference.ownerClass) + PARTIAL_FORMAT.format(backreference.formatPartial() + map.get(reference));
                    }
                    map.put(backreference, backreference.formatPartial() + map.get(reference));
                }
            }
        }
        return "";
    }

    public String formatValue() {
        if (target == null) {
            return NULL_VALUE_FORMAT.format("null");
        }
        String formatted;
        if (target instanceof String) {
            return STRING_VALUE_FORMAT.format(target);
        } else if (Modifier.isFinal(targetClass.getModifiers()) && (target instanceof Number || target instanceof Character || target instanceof Boolean)) {
            return PRIMITIVE_VALUE_FORMAT.format(target);
        } else {
            try {
                if (targetClass.isArray()) {
                    return TYPEHINT_VALUE_FORMAT.format("(" + formatShortClassName(targetClass) + ")") + " " + TOSTRING_VALUE_FORMAT.format(Arrays.toString((Object[]) target));
                } else {
                    return TYPEHINT_VALUE_FORMAT.format("(" + formatShortClassName(targetClass) + ")") + " " + TOSTRING_VALUE_FORMAT.format(target);
                }
            } catch (Exception e) {
                // ignore exceptions during toString
                return DEFAULT_VALUE_FORMAT.format(formatClassName(targetClass) + "@" + Integer.toHexString(System.identityHashCode(target)));
            }
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

    public Object getOwner() {
        return owner;
    }

    public Class<?> getOwnerClass() {
        return ownerClass;
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
}
