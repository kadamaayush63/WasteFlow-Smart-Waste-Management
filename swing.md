# WasteFlow Swing: kahan use hua hai aur kaunse concepts hain?

Is file mein har feature ke saath uski **source-code location**, **concept ka naam**, aur **kyun use hua** diya gaya hai. Main Swing app `desktop/` module mein hai; backend algorithms `backend/` mein hain.

## 1. Swing UI components: location aur concept

Sabhi desktop UI references [WasteFlowDesktop.java](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java) mein hain.

| Code location | Kya use hua hai | Programming / Swing concept | Project mein kaam |
|---|---|---|---|
| [Line 37](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L37) | `JFrame` | Top-level Swing window | “WasteFlow · Operations” desktop window banata hai. |
| [Lines 38–40](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L38) | `DefaultTableModel` | Table model / UI data model | Requests, bins aur route stops ke table columns aur row data manage karta hai. |
| [Lines 41–43](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L41) | `JTable`, `ListSelectionModel` | Tabular UI, single/multiple selection | Requests ko multi-select; bin aur route stop ko select karke dekhne/act karne deta hai. |
| [Lines 44–45](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L44) | `JComboBox` | Choice / selection control | Available truck aur `DIJKSTRA` / `NEAREST_STOP` algorithm chunne ke liye. |
| [Lines 46–51](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L46) | `JButton` | GUI controls | Refresh, simulation tick aur route lifecycle actions chalate hain. |
| [Lines 65–74](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L65) | `SwingUtilities.invokeLater`, `UIManager` | Event Dispatch Thread (EDT), Look and Feel | Swing window ko UI ke sahi thread par banata hai aur system-style appearance set karta hai. |
| [Lines 76–110](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L76) | Window setup, listeners | Event-driven GUI, window lifecycle | Window configure karta hai; button click aur close events ke handlers attach karta hai. |
| [Lines 84–88](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L84) | `JPanel`, `BorderLayout` | Container aur layout manager | Header, main workspace aur footer arrange karta hai. |
| [Lines 112–134](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L112) | `JPanel`, `GridLayout`, `FlowLayout` | GUI composition / layout managers | Title, connection URL aur header controls ko layout karta hai. |
| [Lines 136–170](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L136) | `JSplitPane`, `JPanel`, layouts | Resizable split-pane layout | Left mein requests/route planning; right mein bins/active route dikhata hai. |
| [Lines 173–190](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L173) | Route summary aur action panel | Component composition | Active route ka status aur lifecycle buttons ek jagah rakhta hai. |
| [Lines 202–214](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L202) | `JScrollPane`, `JTable`, `JLabel` | Scrollable table panel | Table headings dikhata hai aur rows zyada hone par scroll allow karta hai. |
| [Lines 399–406](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L399) | `DefaultTableModel` override | Read-only table / polymorphic override | `isCellEditable` ko override karke table cells ko non-editable rakhta hai. |
| [Lines 408–417](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L408) | `JTable` setup | Reusable UI helper, selection configuration | Row height, selection mode, colors aur table-header behavior set karta hai. |
| [Lines 391–396](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L391) | `JOptionPane` | User-facing error dialog | API ya operation error ko operator ke saamne dikhata hai. |

## 2. Screen par data aur actions ka flow

### Data load karna

1. [Line 216](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L216) ka `refresh()` operation chalata hai.
2. [Line 226](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L226) ka `fetchData()` bins, trucks, pending requests aur active route mangta hai.
3. [Line 234](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L234) ka `applyData()` response ko tables aur truck dropdown mein dikhata hai.

### Route plan aur complete karna

1. Operator requests table se requests, dropdown se truck aur algorithm select karta hai.
2. [Line 274](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L274) ka `optimizeRoute()` IDs backend ko bhejta hai.
3. [Line 292](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L292) ka `mutateRoute()` route start/finish API operations call karta hai.
4. [Line 304](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L304) ka `completeSelectedStop()` selected pending stop ko complete karta hai.
5. [Lines 356–388](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L356) busy state, route status aur available actions ke mutabiq buttons enable/disable karte hain.

Simulation button backend ka tick endpoint call karta hai; uske baad data refresh hota hai. Button handlers [lines 90–100](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L90) aur API method [WasteFlowApiClient line 54](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L54) mein hain.

## 3. Swing app mein kaunse Java concepts use hue?

| Concept | Exact location | Is project mein iska matlab |
|---|---|---|
| **Class / object (OOP)** | [WasteFlowDesktop class, line 30](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L30); [API client class, line 17](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L17) | `WasteFlowDesktop` screen aur UI state own karta hai; `WasteFlowApiClient` HTTP communication own karta hai. |
| **Encapsulation** | [Desktop fields, lines 32–57](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L32) | UI components/data fields private rakhe hain; unka behavior class ke methods ke through control hota hai. |
| **Separation of concerns / layered design** | [Desktop `fetchData`, line 226](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L226); [API client `send`, line 86](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L86) | UI display/actions handle karta hai, client HTTP/JSON handle karta hai, backend business rules aur database handle karta hai. |
| **Event-driven programming** | [Button listeners, lines 90–101](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L90) | Button click par handler chalta hai; program operator ke action ke response mein kaam karta hai. |
| **Concurrency / background work** | [SwingWorker, line 330](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L330); [EDT launch, line 65](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L65) | Network requests UI thread se alag chalti hain, taaki window respond karti rahe. UI update Swing thread par hota hai. |
| **Generics / type-safe collections** | [Request list, line 55](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L55); [DashboardData, line 441](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L441) | `List<CollectionRequest>` jaise typed lists galat type ka data add hone se bachate hain. |
| **Java records / data transfer objects** | [API records, lines 131–148](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L131); [DashboardData, line 441](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L441) | JSON response values ko structured Java data objects mein rakhte hain. |
| **Lambda expressions / functional interfaces** | [Button listeners, line 90](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L90); [SupplierWithException, line 432](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L432) | Click handler aur background operation ko concise behavior ke roop mein pass karte hain. |
| **Exception handling** | [API error parsing, line 110](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L110); [SwingWorker completion/error handling, line 330](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L330) | Failed HTTP response ya network error ko success samajhne ke bajay operator tak pahunchata hai. |
| **Validation / state-based UI** | [Stop status guard, line 304](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L304); [Button availability, line 369](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowDesktop.java#L369) | Sirf valid route state/actions allow karta hai; authoritative business validation phir bhi backend karta hai. |

Yeh **MVC-jaisa separation** hai, poora formal Swing MVC implementation nahi: Swing widgets view/control dete hain, desktop class UI state aur event handling karta hai, API client network layer hai aur Spring Boot backend domain rules ka malik hai.

## 4. Backend ke algorithms/data structures: Swing mein nahi, lekin isi app se use hote hain

Swing app request bhejta hai; neeche ke concepts **backend Java code** mein implement hain. Yeh presentation mein Swing implementation se alag batana chahiye.

| Concept | Location | Kahan/kyun use hota hai |
|---|---|---|
| **Dijkstra shortest-path algorithm + `PriorityQueue<Node>`** | [`DijkstraPathFinder.java`, line 19](./backend/src/main/java/com/wasteflow/route/algorithm/DijkstraPathFinder.java#L19) | Sabse chhoti tentative distance wala node pehle process karke graph mein shortest path dhoondhta hai. Route planning ka path-finding hissa hai. |
| **Custom generic singly linked list** | [`RouteService.java`, line 61](./backend/src/main/java/com/wasteflow/route/RouteService.java#L61) | `SinglyLinkedList<CollectionRequest>` route input requests ko manage karta hai. |
| **Linked-list based route stops** | [`RouteService.java`, line 100](./backend/src/main/java/com/wasteflow/route/RouteService.java#L100) | `SinglyLinkedList<RouteStop>` optimized sequence mein stops tayyar karta hai. |
| **Priority-sorted database query** | [`CollectionRequestController.java`, line 24](./backend/src/main/java/com/wasteflow/collection/CollectionRequestController.java#L24) | Request results ko priority descending aur create time ascending order mein return karta hai. Yeh Java `PriorityQueue` nahi; repository/database sorting hai. |

### Queue ke baare mein sahi clarification

- Swing client pending requests ko `JTable` mein dikhata hai; client mein `Queue`, `ArrayDeque` ya `PriorityQueue` object nahi banaya gaya.
- `PriorityQueue` backend ke Dijkstra pathfinder mein hai—yeh **graph nodes ko distance ke order mein process karti hai**, waste requests schedule nahi karti.
- Collection requests API se priority order mein aate hain, lekin woh database/repository ordering hai.
- FIFO Queue aur Circular Queue `doc.md` mein proposed ideas hain; current Swing feature mein implemented nahi hain.

## 5. Swing app aur REST API ke beech connection

[`WasteFlowApiClient`](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java) Java `HttpClient` aur Jackson JSON mapper use karta hai:

| API client location | Endpoint/action |
|---|---|
| [Lines 34–46](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L34) | Bins, trucks, pending requests aur active route GET karta hai. |
| [Line 54](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L54) | `POST /api/simulation/tick`. |
| [Line 58](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L58) | `POST /api/routes/optimize`. |
| [Lines 65–73](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L65) | Route start, stop complete aur route finish endpoints. |
| [Lines 86–110](./desktop/src/main/java/com/wasteflow/desktop/WasteFlowApiClient.java#L86) | HTTP request bhejna, response parse karna aur non-success response ko error banana. |

**Architecture ka reason:** React frontend aur Swing desktop client dono same Spring Boot API use kar sakte hain. Isse route logic ya database code GUI mein duplicate nahi hota.

## 6. Kya abhi implemented nahi hai?

- Swing screen mein road map ya animated truck nahi hai.
- Swing client mein FIFO/Circular Queue scheduling nahi hai.
- Swing se bin create/edit controls abhi nahi hain.
- Request list refresh button ya operations ke baad refresh se update hoti hai; automatic periodic desktop refresh nahi hai.

## 7. Presentation ke liye short explanation

> “Swing ka use WasteFlow ke Java desktop operator dashboard mein kiya hai. JFrame main window hai, JPanel aur layout managers dashboard arrange karte hain, JTable bins/requests/stops dikhati hai, JComboBox truck aur algorithm select karne deta hai aur buttons route actions chalate hain. SwingWorker API calls background mein chalata hai, isliye UI responsive rehti hai. Swing client REST API se Spring Boot backend ko call karta hai. Dijkstra ki PriorityQueue aur route linked lists backend mein hain, Swing UI ke andar nahi.”

## 8. Run

Backend start hone ke baad workspace root se:

```powershell
.\backend\mvnw.cmd -f desktop\pom.xml package
java -jar desktop\target\wasteflow-desktop.jar
```

Run/configuration details [desktop/README.md](./desktop/README.md) mein hain.
