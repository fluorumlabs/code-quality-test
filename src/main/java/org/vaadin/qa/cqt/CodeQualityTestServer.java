package org.vaadin.qa.cqt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.vaadin.qa.cqt.engine.EngineInstance;
import org.vaadin.qa.cqt.suites.CollectionInspections;
import org.vaadin.qa.cqt.suites.FieldInspections;
import org.vaadin.qa.cqt.suites.LambdaInspections;
import org.vaadin.qa.cqt.suites.ResourceInspections;
import org.vaadin.qa.cqt.utils.HtmlFormatter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Path cqtIgnoreRoot = Paths.get("");
    private int port = DEFAULT_PORT;

    public CodeQualityTestServer listenOn(int port) {
        this.port = port;
        return this;
    }

    public CodeQualityTestServer cqtIgnoreRoot(String path) {
        this.cqtIgnoreRoot = Paths.get(path);
        return this;
    }

    public CodeQualityTestServer cqtIgnoreRoot(File path) {
        this.cqtIgnoreRoot = path.toPath();
        return this;
    }

    public CodeQualityTestServer cqtIgnoreRoot(Path path) {
        this.cqtIgnoreRoot = path;
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
        private final List<String> hiddenDescriptors = new CopyOnWriteArrayList<>();
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
                output.println("<!DOCTYPE html><html><base href='.'>");
                output.println("<style>\n" +
                        "    body {\n" +
                        "        font-size: 16px;\n" +
                        "        line-height: 24px;\n" +
                        "        padding: 24px;\n" +
                        "        background-color: #fff;\n" +
                        "        color: #000;\n" +
                        "        box-sizing: border-box;\n" +
                        "    }\n" +
                        "\n" +
                        "    pre {\n" +
                        "        overflow-x: hidden;\n" +
                        "        text-overflow: ellipsis;\n" +
                        "        font-family: \"Fira Code\", \"Source Code Pro\", \"Lucida Console\", Monaco, monospace;\n" +
                        "    }\n" +
                        "\n" +
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
                        "\n" +
                        "    h1 {\n" +
                        "        margin-bottom: 0;\n" +
                        "        font-weight: normal;\n" +
                        "    }\n" +
                        "\n" +
                        "    .report, .banner {\n" +
                        "        font-size: 1.2em;\n" +
                        "    }\n" +
                        "\n" +
                        "    .updated .report .category::before {\n" +
                        "        content: 'New/updated';\n" +
                        "        padding-left: 8px;\n" +
                        "        padding-right: 8px;\n" +
                        "        margin-right: 8px;\n" +
                        "    }\n" +
                        "\n" +
                        "    .updated .report.error .category::before {\n" +
                        "        background-color: #b63a7e;\n" +
                        "        color: #fff;\n" +
                        "        border-radius: 4px;\n" +
                        "    }\n" +
                        "\n" +
                        "    .updated .report.warning .category::before {\n" +
                        "        background-color: #853eec;\n" +
                        "        color: #fff;\n" +
                        "        border-radius: 4px;\n" +
                        "    }\n" +
                        "\n" +
                        "    .updated .report.info .category::before {\n" +
                        "        background-color: #17934f;\n" +
                        "        color: #fff;\n" +
                        "        border-radius: 4px;\n" +
                        "    }\n" +
                        "\n" +
                        "    .banner {\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "\n" +
                        "    .report .message {\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "\n" +
                        "    .report .buttons {\n" +
                        "        opacity: 0;\n" +
                        "        transition: opacity 0.2s;\n" +
                        "    }\n" +
                        "\n" +
                        "    .block {\n" +
                        "        display: block;\n" +
                        "        overflow-x: hidden;\n" +
                        "        text-overflow: ellipsis;\n" +
                        "    " +
                        "}\n" +
                        "\n" +
                        "    .block:hover .buttons {\n" +
                        "        opacity: 1;\n" +
                        "    }\n" +
                        "\n" +
                        "    .value.null, .error, .exception {\n" +
                        "        color: #b63a7e;\n" +
                        "    }\n" +
                        "\n" +
                        "    .warning {\n" +
                        "        color: #853eec;\n" +
                        "    }\n" +
                        "\n" +
                        "    .value.primitive {\n" +
                        "        color: #356bd4;\n" +
                        "    }\n" +
                        "\n" +
                        "    .title {\n" +
                        "        font-size: 1.2rem;\n" +
                        "        display: block;\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "\n" +
                        "    .buttons a {\n" +
                        "        border: currentColor solid 1px;\n" +
                        "        padding-left: 8px;\n" +
                        "        padding-right: 8px;\n" +
                        "        text-decoration: none;\n" +
                        "        background-color: #356bd400;\n" +
                        "        border-radius: 4px;\n" +
                        "        display: inline-block;\n" +
                        "    }\n" +
                        "\n" +
                        "    .buttons a:hover {\n" +
                        "        background-color: #356bd420;\n" +
                        "    }\n" +
                        "\n" +
                        "    a, a * {\n" +
                        "        color: currentColor;\n" +
                        "        transition: color 200ms, background-color 200ms;\n" +
                        "    }\n" +
                        "\n" +
                        "    a:hover, a:hover * {\n" +
                        "        color: #356bd4 !important;\n" +
                        "    }\n" +
                        "\n" +
                        "    .info, .value.string {\n" +
                        "        color: #17934f;\n" +
                        "    }\n" +
                        "\n" +
                        "    .class {\n" +
                        "        color: black;\n" +
                        "        font-weight: bold;\n" +
                        "    }\n" +
                        "\n" +
                        "    .package, .class.package, .generic, .reference, .value {\n" +
                        "        color: #888;\n" +
                        "        font-weight: normal;\n" +
                        "    }\n" +
                        "\n" +
                        "    .static {\n" +
                        "        font-style: italic;\n" +
                        "    }\n" +
                        "\n" +
                        "    .value.possible, .value.possible .class {\n" +
                        "        color: #356bd4;\n" +
                        "        font-weight: normal;\n" +
                        "        font-style: italic;\n" +
                        "    }\n" +
                        "</style>");
                output.println("</head><body><pre>");

                preamble(output);

                String path = exchange.getRequestURI().getPath();

                try {
                    boolean needToRunScanner = !resultsAreReady.get() && !scannerIsRunning.get();
                    if (path.endsWith("/scan") || needToRunScanner) {
                        output.println("Scanner is currently running...");
                        runScanner(output);
                    } else if (!resultsAreReady.get()) {
                        if (scannerIsRunning.get()) {
                            output.println("Scanner is currently running...");
                        } else {
                            output.println("Previous scanning completed exceptionally. <span class='buttons'></span><a href='scan'>Click to rescan</a></span>");
                        }
                        refreshWhenScannerIsDone(output);
                    } else {
                        if (path.endsWith("/dismiss")) {
                            path = path.substring(0, path.lastIndexOf("/dismiss"));
                            String whatToDismiss = exchange.getRequestURI().getQuery().replace('+',' ');
                            appendDismissedReportDescriptor(whatToDismiss);
                        }

                        if (scannerIsRunning.get()) {
                            output.println("Last scanned at " + lastScanDate + ". Scanner is currently running...");
                        } else {
                            output.println("Last scanned at " + lastScanDate + ". <span class='buttons'><a href='scan'>Click to rescan</a></span>");
                        }
                        output.println();

                        // Strip
                        if (path.startsWith("/")) {
                            path = path.substring(1);
                        }
                        if (path.endsWith("/")) {
                            path = path.substring(0, path.length() - 1);
                        }

                        printResults(output, path);
                        if (scannerIsRunning.get()) {
                            refreshWhenScannerIsDone(output);
                        }
                    }
                } catch (Exception e) {
                    output.println("<div class='exception'>");
                    e.printStackTrace(output);
                    output.println("</div>");
                    cancelAutoRefresh(output);
                }

                output.println("</pre>");
                output.println("</body></html>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Set<String> readDismissedReportDescriptors() {
            Path cqtIgnore = cqtIgnoreRoot.resolve(".cqtignore");
            if (Files.exists(cqtIgnore)) {
                try (Stream<String> lines = Files.lines(cqtIgnore, StandardCharsets.UTF_8)){

                    return lines.filter(s -> !s.startsWith("#"))
                            .collect(Collectors.toSet());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return Collections.emptySet();
        }

        private void appendDismissedReportDescriptor(String whatToDismiss) {
            Path cqtIgnore = cqtIgnoreRoot.resolve(".cqtignore");
            try {
                Files.write(cqtIgnore, Collections.singletonList(whatToDismiss), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void refreshWhenScannerIsDone(PrintWriter output) {
            synchronized (scanner) {
                refresh(output);
            }
        }

        private void printResults(PrintWriter output, String path) {
            Set<String> dismissed = readDismissedReportDescriptors();

            if (!path.isEmpty()) {
                try {
                    Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(path);
                    output.println("<span class='title'><span class='buttons'><a href='/'>&lt;</a></span> Reports for " + Reference.formatClassName(aClass) + "</span>");
                } catch (Throwable e) {
                    output.println("<span class='title error'><span class='buttons'><a href='/'>&lt;</a></span> Unknown class " + path + "</span>");
                    return;
                }
            }
            synchronized (resultLockObject) {
                if (path.isEmpty()) {
                    output.println("<span class='title'>All reports</span>");
                }
                boolean foundSomething = false;

                for (String currentResult : currentResults) {
                    if (!path.isEmpty()) {
                        if (!currentResult.contains("<!-- Class: " + HtmlFormatter.encodeValue(path))) {
                            continue;
                        }
                    }

                    if (dismissed.stream().anyMatch(desc -> currentResult.contains("<!-- Descriptor: "+HtmlFormatter.encodeValue(desc)+" -->"))) {
                        continue;
                    }

                    foundSomething = true;
                    if (previousResults.contains(currentResult)) {
                        output.println("<span class='block same'>" + currentResult + "</span>");
                    } else {
                        output.println("<span class='block updated'>" + currentResult + "</span>");
                    }
                }

                if (!foundSomething) {
                    output.println("<span class='title info'>All is good</span>");
                }
            }
        }

        private void preamble(PrintWriter output) {
            output.println("<span class='banner'>Code Quality Test Server (" + EngineInstance.get().getName() + ")</span>");
            output.println();
        }

        private void refresh(PrintWriter output) {
            output.print("<script>self.location.replace(document.baseURI)</script>");
        }

        private void autoRefresh(PrintWriter output) {
            output.print("<script>\n" +
                    "    function autoRefresh() {" +
                    " " +
                    "self.location.replace(document.baseURI); }\n" +
                    "    self.window.addEventListener('DOMContentLoaded', autoRefresh);\n" +
                    "</script>");
        }

        private void cancelAutoRefresh(PrintWriter output) {
            output.print("<script>\n" +
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

                    scanner.visitEngine();
                    if (scanTargets.isEmpty()) {
                        scanner.visitClassLoaders();
                    } else {
                        scanTargets.forEach(scanner::visit);
                    }

                    // Copy prev/new results

                    List<String> newReports = scanner.analyze().stream()
                            .sorted(Comparator.comparing(InspectionResult::getLevel).reversed()
                                    .thenComparing(InspectionResult::getCategory)
                                    .thenComparing(InspectionResult::getMessage))
                            .flatMap(ir -> {
                                return ir.format().stream().sorted();
                            })
                            .collect(Collectors.toList());

                    synchronized (resultLockObject) {
                        previousResults.clear();
                        previousResults.addAll(currentResults);
                        currentResults.clear();
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