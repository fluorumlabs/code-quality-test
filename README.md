# Code Quality Test
 
Code Quality Test (or CQT) is an in-process static state analyzer. It uses a
combination of application state and bytecode analysis to find potential
problems, such as concurrency issues or mutable singleton objects.
  
It differs from the usual FindBugs/SpotBugs and SonarQube in that CQT
is executed in the running application, thus utilizing the knowledge of
instantiated objects and their relationship.

It has minimum dependencies (only `common-text`) and is framework-agnostic, 
even though it has built-in support for Spring-, Vaadin- or Servlet-based
applications.

## Usage

- Add the following lines to your `pom.xml`:

```xml
<dependency>
    <groupId>org.vaadin.qa</groupId>
    <artifactId>code-quality-test</artifactId>
    <version>1.0.0</version>
</dependency>
```  

- And the following to your `main()` method (or any other method
that is invoked during application startup):

```java
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new CodeQualityTestServer()
                .includePackages("my.application.package")
                .start();

        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
    }

}
```

- Start your application and navigate to `http://localhost:8777` 
once CQT server starts.

- Perform some actions in your application.

- Hit `Rescan` button in CQT UI and see if there are any new reports.

## Configuration

CQT can be configured before server is started as follows:
```java
new CodeQualityTestServer()

        /* Which port CQT server will listen to. Defaults to 8777 */
        .listenOn(8777)

        /* Where to create .cqtignore file for suppressed reports. Defaults to current directory */
        .cqtIgnoreRoot(Paths.get("target"))

        /* Control fields of which classes would be reported */
        .include(Servlet.class::isAssignableFrom)                   
        .includePackages("my.application.example")
        .exclude(clazz -> clazz.isAnnotationPresent(Entity.class))
        .excludePackages("my.application.example.dto")

        /* Specify scope for specific classes */
        .withScopeHint(MyService.class, "singleton")

        /* Run only selected inspection suites. If no specified, all built-in CQT suites are used */
        .withSuites(ResourceInspections::new)

        /* Use specific object as an entry point for scanner. Class loaders are used if not specified */
        .scanTargets(SomeClass::new)

        .start();
```

## `.cqtignore`

It is possible to suppress specific reports using CQT UI. The suppressed reports will be written
to a `.cqtignore` file, which can be committed to your project repository if needed.

## Inspections

### Collection inspections

- **Advice: Store ConcurrentHashMap in fields of type ConcurrentHashMap**

  ConcurrentHashMap (unlike the generic ConcurrentMap interface) guarantees that the lambdas passed into compute()-like methods are performed atomically per key, and the thread safety of the class may depend on that guarantee. If used in conjunction with a static analysis rule that prohibits calling compute()-like methods on ConcurrentMap-typed objects that are not ConcurrentHashMap it could prevent some bugs: e. g. calling compute() on a ConcurrentSkipListMap might be a race condition and it’s easy to overlook that for somebody who is used to rely on the strong semantics of compute() in ConcurrentHashMap.
  See: https://github.com/code-review-checklists/java-concurrency#chm-type
  
- **Advice: Store ConcurrentSkipListMap in fields of type ConcurrentMap or ConcurrentSkipListMap**

  By explicitly specifying type of the fields, it would be easier to spot problematic patterns like following:
   
   ConcurrentMap<String, Entity> entities = getEntities();
   if (!entities.containsKey(key)) {
       entities.put(key, entity);
   } else {
       ...
   }
   
  It should be pretty obvious that there might be a race condition because an entity may be put into the map by a concurrent thread between the calls to containsKey() and put() (see https://github.com/code-review-checklists/java-concurrency#chm-race about this type of race conditions). While if the type of the entities variable was just Map<String, Entity> it would be less obvious and readers might think this is only slightly suboptimal code and pass by.
  
  See: https://github.com/code-review-checklists/java-concurrency#concurrent-map-type
  
- **Advice: Use non-blocking collection instead of blocking**
  - Use non-blocking ConcurrentHashMap instead of Hashtable
  - Use non-blocking ConcurrentHashMap instead of Collections.synchronizedMap(HashMap)
  - Use non-blocking ConcurrentHashMap.newKeySet() instead of Collections.synchronizedSet(HashSet)
  - Use non-blocking ConcurrentLinkedDeque instead of LinkedBlockingDeque
  - Use non-blocking ConcurrentLinkedQueue instead of LinkedBlockingQueue
  - Use non-blocking CopyOnWriteArrayList instead of Collections.synchronizedList(ArrayList)
  - Use non-blocking CopyOnWriteArrayList instead of Vector
  - Use non-blocking ConcurrentSkipListMap instead of Collections.synchronizedMap(TreeMap)
  - Use non-blocking ConcurrentSkipListSet instead of Collections.synchronizedMap(TreeSet)
  
  Note: ConcurrentSkipListMap is not the state of the art concurrent sorted dictionary implementation.

- **Advice: Use EnumMap and EnumMap instead of Map<Enum, ...> and Set<Enum>**

- **Suggestion: Consider using ClassValue instead of Map<Class, ...>**

  Note, however, that unlike ConcurrentHashMap with its computeIfAbsent() method ClassValue doesn’t guarantee that per-class value is computed only once, i. e. ClassValue.computeValue() might be executed by multiple concurrent threads. So if the computation inside computeValue() is not thread-safe, it should be synchronized separately. On the other hand, ClassValue does guarantee that the same value is always returned from ClassValue.get() (unless remove() is called).

  See: https://github.com/code-review-checklists/java-concurrency#use-class-value

- **Warning: Mutable collection exposed via non-private field or non-private getter**

  Exposing mutable collection allows the state of the shared object to be mutated externally.

- **Probable Error: Non-thread-safe mutable collection is used**

  Using non-thread-safe mutable collections in a shared environment can cause hard to track race conditions and spontaneous ConcurrentModificationException exceptions

### Field inspections

- **Warning: Non-final field can be modified: stateful shared object**

  Non-final fields make service stateful and allow the state to be mutated externally.
  
  Care must be taken to properly initialize fields of static, singleton- and session-scoped objects. Consider using double-checked locking following SafeLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/SafeLocalDCL.java#l71), or UnsageLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/UnsafeLocalDCL.java#l71).
  
  See: https://github.com/code-review-checklists/java-concurrency#safe-local-dcl
  
- **Warning: Actual value of non-transient field in serializable object is not Serializable**

  All non-static fields of object implementing Serializable must be either serializable or transient. Failure to do so will lead to NotSerializableException when an attempt to serialize will be made.
  
- **Probable Error: Not thread-safe class**

  Non-thread-safe classes must not be used in a shared environment.
  
- **Probable Error: Transient field is not initialized after deserialization**

  Transient field is not initialized after deserialization. When Serializable object is deserialized, it's constructor is not called and all transient fields have no value. If transient field is final or is initialized only from constructor, this will leave object in incomplete state.
  
### Resource management inspections

- **Probable Error: AutoClosable resource stored in a field**

  AutoCloseable resource stored in a field makes it impossible to use try-with-resources pattern and might result in resource leak.
  
- **Warning: ClassLoader stored in a static field**

  In a dynamic system like an application server or OSGI, you should take good care not to prevent ClassLoader from garbage collection. As you undeploy and redeploy individual applications in an application server you create new class loaders for them. The old ones are unused and should be collected. Java isn't going to let that happen if there is a single dangling reference from container code into your application code.
  
  See: https://www.odi.ch/prog/design/newbies.php#56
  
- **Warning: Class stored in static field**

  In a dynamic system like an application server or OSGI, you should take good care not to prevent ClassLoader from garbage collection. As you undeploy and redeploy individual applications in an application server you create new class loaders for them. The old ones are unused and should be collected. Java isn't going to let that happen if there is a single dangling reference from container code into your application code.
  
  Note that Class stores reference to ClassLoader internally.
  
  See: https://www.odi.ch/prog/design/newbies.php#56

- **Probable Error: ExecutorService resource stored in a field**

  ExecutorService is a resource and must be closed explicitly via try-with-resources or try-finally statement. Failure to shutdown an ExecutorService might lead to a thread leak even if an ExecutorService object is no longer accessible, because some implementations (such as ThreadPoolExecutor) shutdown themselves in a finalizer, while finalize() is not guaranteed to ever be called by the JVM. To make explicit shutdown possible, first, ExecutorService objects must not be assinged into variables and fields of Executor type.
  
  See: https://github.com/code-review-checklists/java-concurrency#explicit-shutdown
  
- **Probable Error: ThreadLocal value is not removed after work is done**

  If one of the application classes stores a value in ThreadLocal variable and doesn’t remove it after the task at hand is completed, a copy of that Object will remain with the Thread (from the application server thread pool). Since lifespan of the pooled Thread surpasses that of the application, it will prevent the object and thus a ClassLoader being responsible for loading the application from being garbage collected. And we have created a leak, which has a chance to surface in a good old java.lang.OutOfMemoryError: PermGen space form.
  
  See: https://plumbr.io/blog/locked-threads/how-to-shoot-yourself-in-foot-with-threadlocals
  
- **Warning: Thread must not be managed by short-lived objects**

  It possible to reuse executors by creating them one level up the stack and passing shared executors to constructors of the short-lived objects, or a shared ExecutorService stored in a static field.
  
  See: https://github.com/code-review-checklists/java-concurrency#reuse-threads
  
- **Probable Error: Spawning thread from static initializers**

  Timer spwans a new thread in its constructor. The new Thread will inherit some properties from its parent: context classloader, inheritable ThreadLocals, and some security properties (access rights). It is therefore rarely desireable to have those property set in an uncontrolled way. This may for instance prevent GC of a class loader. The static initializer is executed by the thread that first loads the class (in any given ClassLoader), which may be a totally random thread from a thread pool of a webserver for example. If you want to control these thread properties you will have to start threads in a static method, and take control of who is calling that method.
  
  See: https://www.odi.ch/prog/design/newbies.php#54
  
- **Advice: Use a ForkJoinPool instead of a ThreadPoolExecutor with N threads**

  ForkJoinPool is more scalable because internally it maintains one queue per each worker thread, whereas ThreadPoolExecutor has a single, blocking task queue shared among all threads. ForkJoinPool implements ExecutorService as well as ThreadPoolExecutor, so could often be a drop in replacement.
  
  See: https://github.com/code-review-checklists/java-concurrency#fjp-instead-tpe
  
### Lambda inspections

- **Warning: Narrow-scoped object captured in effectively session-, singleton- or static- lambda**

  Capturing short-lived objects in a closure of session-scoped lambdas will extend their lifetime and, potentially may result in memory leaks.