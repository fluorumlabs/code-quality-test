package org.vaadin.qa.cqt;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public class InspectionResult {
    private final Inspection inspection;
    private final List<Reference> references = new ArrayList<>();

    public InspectionResult(Inspection inspection) {
        this.inspection = inspection;
    }

    public void add(Reference reference) {
        references.add(reference);
    }

    public boolean isEmpty() {
        return references.isEmpty();
    }

    public List<String> format() {
        if (references.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();

        Map<String, List<Reference>> referenceGroup = new HashMap<>();
        for (Reference reference : references) {
            referenceGroup.computeIfAbsent(reference.formatReference(), str -> new ArrayList<>()).add(reference);
        }

        List<String> groups = referenceGroup.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String group : groups) {
            StringBuilder output = new StringBuilder();

            output.append(String.format("%s: %s\n", inspection.getCategory(), escapeHtml4(inspection.getMessage())));
            List<Reference> references = referenceGroup.get(group);
            Reference first = references.iterator().next();
            String contextPath = first.formatPathToContext();
            output.append(String.format("\t\tClass:         %s\n", first.formatOwnerClassWithLink()));
            output.append(String.format("\t\tContext:       %s\n", escapeHtml4(first.formatScope())));
            if (!contextPath.isEmpty()) {
                output.append(String.format("\t\tContext path:  %s\n", escapeHtml4(contextPath)));
            }
            output.append(String.format("\t\tField:         %s\n", escapeHtml4(first.formatField())));
            output.append(String.format("\t\tValue:         %s\n", first.formatValue()));
            if (references.size()>1) {
                output.append(String.format("\t\t               ... %d other\n", references.size()-1));
            }
            List<String> backrefs = references.stream().flatMap(reference -> reference.formatBackreferences().stream())
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            int counter = 0;
            for (String backref : backrefs) {
                if (counter == 0) {
                    output.append(String.format("\t\tReferenced by: %s\n", backref));
                } else {
                    output.append(String.format("\t\t               %s\n", backref));
                }
                counter++;
                if (counter>first.getScanner().getMaxReferences()) {
                    output.append(String.format("\t\t               ... %d more\n", backrefs.size()-counter));
                    break;
                }
            }

            results.add(output.toString());
        }
        return results;
    }
}
