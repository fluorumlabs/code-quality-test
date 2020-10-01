package org.vaadin.qa.cqt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.vaadin.qa.cqt.engine.EngineInstance;
import org.vaadin.qa.cqt.suites.CollectionInspections;
import org.vaadin.qa.cqt.suites.FieldInspections;
import org.vaadin.qa.cqt.suites.LambdaInspections;
import org.vaadin.qa.cqt.suites.ResourceInspections;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class ScannerServer {
    private Predicate<Class<?>> includes = x -> false;
    private Predicate<Class<?>> excludes = x -> false;
    private int port = 8777;
    private Map<Class<?>, String> scopeHints = new HashMap<>();
    private List<Supplier<Suite>> suites = new ArrayList<>();
    private List<Supplier<Object>> scanTargets = new ArrayList<>();

    public ScannerServer() {
    }

    public ScannerServer includePackages(String... strings) {
        includes = includes.or(
                Stream.of(strings)
                        .<Predicate<Class<?>>>map(p -> (clazz -> clazz.getName().startsWith(p)))
                        .reduce(Predicate::or)
                        .orElse(x -> true)
        );
        return this;
    }

    public ScannerServer include(Predicate<Class<?>> filter) {
        includes = includes.or(filter);
        return this;
    }

    public ScannerServer excludePackages(String... strings) {
        excludes = excludes.or(
                Stream.of(strings)
                        .<Predicate<Class<?>>>map(p -> (clazz -> clazz.getName().startsWith(p)))
                        .reduce(Predicate::or)
                        .orElse(x -> true)
        );
        return this;
    }

    public ScannerServer exclude(Predicate<Class<?>> filter) {
        includes = includes.or(filter);
        return this;
    }

    public ScannerServer withSuites(Supplier<Suite>... supplier) {
        suites.addAll(Arrays.asList(supplier));
        return this;
    }

    public ScannerServer scanTargets(Supplier<Object>... supplier) {
        scanTargets.addAll(Arrays.asList(supplier));
        return this;
    }

    public ScannerServer withScopeHint(Class<?> clazz, String scope) {
        scopeHints.put(clazz, scope);
        return this;
    }

    public void start() {
        String property = System.getProperty("cqt-started");
        if (!"true".equals(property)) {
            System.setProperty("cqt-started", "true");
            scopeHints.forEach(ScopeDetector::hint);
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", new MyHandler());
                server.setExecutor(null);
                server.start();
                String banner = "===== CQT server running at http://localhost:" + port + " =====";
                String separator = new String(new char[banner.length()]).replace("\0", "=");
                System.out.println(separator);
                System.out.println(banner);
                System.out.println(separator);
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error starting CQT server");
                e.printStackTrace();
            }
        }
    }

    private class MyHandler implements HttpHandler {
        private final Set<String> previousResults = ConcurrentHashMap.newKeySet();
        private final Scanner scanner = new Scanner(includes.and(excludes.negate()));

        public MyHandler() {
            if (suites.isEmpty()) {
                scanner.addSuite(CollectionInspections::new);
                scanner.addSuite(LambdaInspections::new);
                scanner.addSuite(ResourceInspections::new);
                scanner.addSuite(FieldInspections::new);
            } else {
                suites.forEach(scanner::addSuite);
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, 0);

            try (PrintWriter output = new PrintWriter(exchange.getResponseBody(), true)) {
                output.println("<style>a { text-decoration: none; }</style>");
                output.println("<pre>");

                try {
                    if (exchange.getRequestURI().getPath().equals("/") || exchange.getRequestURI().getPath().isEmpty()) {
                        runScanner(output);
                    } else {
                        showObject(output, exchange.getRequestURI().getPath().substring(1));
                    }
                } catch (Exception e) {
                    output.println("<strong>");
                    e.printStackTrace(output);
                    output.println("</strong>");
                }

                output.println("</pre>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void showObject(PrintWriter output, String hash) {
            synchronized (scanner) {
                scanner.setOutput(output);
                scanner.dumpObject(hash);
            }
        }

        private void runScanner(PrintWriter output) {
            synchronized (scanner) {
                output.println("Using engine: " + EngineInstance.get().getName());

                scanner.setOutput(output);
                scanner.clear();

                if (scanTargets.isEmpty()) {
                    scanner.visitClassLoaders();
                } else {
                    scanTargets.forEach(scanner::visit);
                }

                output.println();

                List<String> newReports = scanner.analyze().stream()
                        .flatMap(ir -> ir.format().stream())
                        .filter(s -> !s.isEmpty())
                        .sorted()
                        .collect(Collectors.toList());

                List<String> oldReports = new ArrayList<>();
                for (String report : newReports) {
                    if (previousResults.contains(report)) {
                        oldReports.add(report);
                    }
                }
                newReports.removeAll(oldReports);

                if (!newReports.isEmpty()) {
                    output.println("<strong>");
                    output.println(String.join("\n", newReports));
                    output.println("</strong>\n");
                }

                if (!oldReports.isEmpty()) {
                    output.println(String.join("\n", oldReports));
                }

                previousResults.clear();
                previousResults.addAll(oldReports);
                previousResults.addAll(newReports);

                output.println();
            }
        }
    }

}
