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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Created by Artem Godin on 9/28/2020.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CodeQualityTestServer {
    private static final int DEFAULT_PORT = 8777;
    private final Map<Class<?>, String> scopeHints = new HashMap<>();
    private final List<Supplier<Suite>> suites = new ArrayList<>();
    private final List<Supplier<Object>> scanTargets = new ArrayList<>();
    private Predicate<Class<?>> includes = x -> false;
    private Predicate<Class<?>> excludes = x -> false;
    private int port = DEFAULT_PORT;

    public CodeQualityTestServer listenOn(int port) {
        this.port = port;
        return this;
    }

    public CodeQualityTestServer includePackages(String... strings) {
        includes = includes.or(
                Stream.of(strings)
                        .<Predicate<Class<?>>>map(p -> (clazz -> clazz.getName().startsWith(p)))
                        .reduce(Predicate::or)
                        .orElse(x -> true)
        );
        return this;
    }

    public CodeQualityTestServer include(Predicate<Class<?>> filter) {
        includes = includes.or(filter);
        return this;
    }

    public CodeQualityTestServer excludePackages(String... strings) {
        excludes = excludes.or(
                Stream.of(strings)
                        .<Predicate<Class<?>>>map(p -> (clazz -> clazz.getName().startsWith(p)))
                        .reduce(Predicate::or)
                        .orElse(x -> true)
        );
        return this;
    }

    public CodeQualityTestServer exclude(Predicate<Class<?>> filter) {
        includes = includes.or(filter);
        return this;
    }

    public CodeQualityTestServer withSuites(Supplier<Suite>... supplier) {
        suites.addAll(Arrays.asList(supplier));
        return this;
    }

    public CodeQualityTestServer scanTargets(Supplier<Object>... supplier) {
        scanTargets.addAll(Arrays.asList(supplier));
        return this;
    }

    public CodeQualityTestServer withScopeHint(Class<?> clazz, String scope) {
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
                server.setExecutor(Executors.newFixedThreadPool(5));
                server.start();
                String preamble = "Code Quality Test Server (" + EngineInstance.get().getName() + ")";
                String banner = "Server running at http://localhost:" + port;
                int width = Math.max(preamble.length(), banner.length());
                String separator = new String(new char[width]).replace('\0', '*');
                String filler = new String(new char[width]).replace('\0', ' ');
                String fillerPreamble = new String(new char[Math.max(width - preamble.length(), 0)]).replace('\0', ' ');
                String fillerBanner = new String(new char[Math.max(width - banner.length(), 0)]).replace('\0', ' ');
                System.out.println("**" + separator + "**");
                System.out.println("* " + preamble + fillerPreamble + " *");
                System.out.println("* " + filler + " *");
                System.out.println("* " + banner + fillerBanner + " *");
                System.out.println("**" + separator + "**");
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error starting CQT server");
                e.printStackTrace();
            }
        }
    }

    private class MyHandler implements HttpHandler {
        private final List<String> previousResults = new CopyOnWriteArrayList<>();
        private final List<String> currentResults = new CopyOnWriteArrayList<>();
        private final Scanner scanner = new Scanner(includes.and(excludes.negate()));

        private final Object resultLockObject = new Object();

        private final AtomicBoolean resultsAreReady = new AtomicBoolean(false);
        private final AtomicBoolean scannerIsRunning = new AtomicBoolean(false);

        private volatile String lastScanDate = "";

        private MyHandler() {
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
                output.println("<style>\n" +
                        "    @import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;700&display=swap');\n" +
                        "    \n" +
                        "    body {\n" +
                        "        font-size: 16px;\n" +
                        "        line-height: 24px;\n" +
                        "        padding: 24px;\n" +
                        "        background-color: #fff;\n" +
                        "        color: #000;\n" +
                        "    }\n" +
                        "    \n" +
                        "    pre {\n" +
                        "        overflow-x: hidden;\n" +
                        "        text-overflow: ellipsis;\n" +
                        "        font-family: 'Fira Code', monospace;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .container {\n" +
                        "        white-space: normal;\n" +
                        "        position: absolute;\n" +
                        "        width: calc(100vw - 32px);\n" +
                        "        height: 100vh;\n" +
                        "    " +
                        "}\n" +
                        "\n" +
                        "    .container .frame {\n" +
                        "        white-space: pre;\n" +
                        "        background-color: white;\n" +
                        "        position: absolute;\n" +
                        "        top: 0;\n" +
                        "        left: 0;\n" +
                        "        width: 100%;\n" +
                        "        height: 100%;\n" +
                        "    }\n" +
                        "    \n" +
                        "    h1 {\n" +
                        "        margin-bottom: 0;\n" +
                        "        font-weight: normal;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .report, .banner {\n" +
                        "        font-size: 1.2em;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .banner {\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .report .message {\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .report .id {\n" +
                        "        color: #888;\n" +
                        "        opacity: 0;\n" +
                        "        transition: opacity 0.3s;\n" +
                        "        transition-delay: 0.1s;\n" +
                        "    }\n" +
                        "\n" +
                        "    .report:hover .id {\n" +
                        "        opacity: 1;\n" +
                        "    }\n" +
                        "\n" +
                        "    .value.null, .error, .exception {\n" +
                        "        color: #b63a7e;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .warning {\n" +
                        "        color: #853eec;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .value.primitive, a {\n" +
                        "        color: #356bd4;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .info, .value.string {\n" +
                        "        color: #17934f;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .class {\n" +
                        "        color: black;\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .package, .class.package, .generic, .reference, .value {\n" +
                        "        color: #888;\n" +
                        "        font-weight: normal;\n" +
                        "    }\n" +
                        "    \n" +
                        "    .static {\n" +
                        "        font-style: italic;\n" +
                        "    }\n" +
                        "</style>");
                output.println("<pre>");

                preamble(output);

                String path = exchange.getRequestURI().getPath();

                try {
                    boolean needToRunScanner = !resultsAreReady.get() && !scannerIsRunning.get();
                    if (path.equals("/scan") || needToRunScanner) {
                        output.println("Scanner is currently running...");
                        runScanner(output);
                    } else if (!resultsAreReady.get()) {
                        if (scannerIsRunning.get()) {
                            output.println("Scanner is currently running...");
                        } else {
                            output.println("Previous scanning completed exceptionally. <a href='/scan'>Click to rescan</a>");
                        }
                        refreshWhenScannerIsDone(output);
                    } else {
                        if (scannerIsRunning.get()) {
                            output.println("Last scanned at " + lastScanDate + ". Scanner is currently running...");
                        } else {
                            output.println("Last scanned at " + lastScanDate + ". <a href='/scan'>Click to rescan</a>");
                        }
                        output.println();
                        printResults(output);
                        if (scannerIsRunning.get()) {
                            refreshWhenScannerIsDone(output);
                        }
                    }
                } catch (Exception e) {
                    output.println("<div class='exception'>");
                    e.printStackTrace(output);
                    output.println("</div>");
                }

                output.println("</pre>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void refreshWhenScannerIsDone(PrintWriter output) {
            synchronized (scanner) {
                refresh(output);
            }
        }

        private void printResults(PrintWriter output) {
            synchronized (resultLockObject) {
                if (!currentResults.isEmpty()) {
                    output.println("<h1>New/updated reports (" + currentResults.size() + ")</h1>");
                    output.println(String.join("\n", currentResults));
                }
                if (!previousResults.isEmpty()) {
                    output.println("<h1>Previous reports (" + previousResults.size() + ")</h1>");
                    output.println(String.join("\n", previousResults));
                }
                if (currentResults.isEmpty() && previousResults.isEmpty()) {
                    output.println("<h1>All is good</h1>");
                }
            }
        }

        private void preamble(PrintWriter output) {
            output.println("<span class='banner'>Code Quality Test Server (" + EngineInstance.get().getName() + ")</span>");
            output.println();
        }

        private void refresh(PrintWriter output) {
            output.println("<script>self.location.replace('/')</script>");
        }

        private void autoRefresh(PrintWriter output) {
            output.println("<script>\n" +
                    "    function autoRefresh() {" +
                    " " +
                    "self.location.replace('/'); }\n" +
                    "    self.window.addEventListener('DOMContentLoaded', autoRefresh);\n" +
                    "</script>");
        }

        private void cancelAutoRefresh(PrintWriter output) {
            output.println("<script>\n" +
                    "    self.window.removeEventListener('DOMContentLoaded', autoRefresh);\n" +
                    "</script>");
        }

        private void runScanner(PrintWriter output) {
            synchronized (scanner) {
                scannerIsRunning.set(true);
                output.println();
                try {
                    autoRefresh(output);
                    scanner.setOutput(output);
                    scanner.clear();

                    if (scanTargets.isEmpty()) {
                        scanner.visitClassLoaders();
                    } else {
                        scanTargets.forEach(scanner::visit);
                    }

                    // Copy prev/new results

                    Set<String> previousResultSet = new HashSet<>(previousResults);
                    previousResultSet.addAll(currentResults);

                    List<String> newReports = scanner.analyze().stream()
                            .sorted(Comparator.comparing(InspectionResult::getLevel).reversed()
                                    .thenComparing(InspectionResult::getCategory)
                                    .thenComparing(InspectionResult::getMessage))
                            .flatMap(ir -> {
                                return ir.format().stream().sorted();
                            })
                            .collect(Collectors.toList());

                    List<String> oldReports = new ArrayList<>();
                    for (String report : newReports) {
                        if (previousResultSet.contains(report)) {
                            oldReports.add(report);
                        }
                    }
                    newReports.removeAll(oldReports);


                    synchronized (resultLockObject) {
                        previousResults.clear();
                        currentResults.clear();
                        previousResults.addAll(oldReports);
                        currentResults.addAll(newReports);
                    }

                    LocalDateTime dateTime = LocalDateTime.now(); // Gets the current date and time
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    lastScanDate = dateTime.format(formatter);
                    resultsAreReady.set(true);
                } catch (Exception e) {
                    cancelAutoRefresh(output);
                    output.println("<span class='error'>");
                    e.printStackTrace(output);
                    output.println("</span>");
                } finally {
                    scannerIsRunning.set(false);
                }
            }
        }
    }

}
