# Programming Lab Tasks 1 and 2 Implementation

## Project

**WasteFlow** is a waste-bin collection and route-planning application. It tracks bin fill levels and collection requests, then plans truck stops. The real-world problem and application overview are described in [explanation.md](explanation.md#1-project-overview) and [explanation.md](explanation.md#2-problem-being-solved).

## Task 1: Arrays and Basic Operations

### Concepts and locations

| Task 1 concept | How it is used | Source location |
|---|---|---|
| Array storage | Bin responses loaded from the repository are converted to `WasteBinResponse[]`. | [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L26-L30) |
| Display | If there is no search query, array contents are returned for display. The frontend renders bins as table rows. | [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L32-L33), [App.tsx](frontend/src/App.tsx#L137-L143) |
| Search | The service scans the array and matches a query against bin code or location name. | [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L35-L43) |
| Insert | The API validates a new bin and saves it through the repository. | [WasteBinController.java](backend/src/main/java/com/wasteflow/bin/WasteBinController.java#L38-L42), [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L52-L62) |
| Update | The API loads a bin, updates its values, and saves it through the repository. | [WasteBinController.java](backend/src/main/java/com/wasteflow/bin/WasteBinController.java#L44-L46), [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L64-L71) |
| Delete | The API deletes a bin through the repository. Deletion is rejected if a route or request depends on it. | [WasteBinController.java](backend/src/main/java/com/wasteflow/bin/WasteBinController.java#L49-L52), [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L73-L79) |
| Read a single record | A bin is retrieved by its ID and returned to the API. | [WasteBinController.java](backend/src/main/java/com/wasteflow/bin/WasteBinController.java#L33-L36), [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L47-L49) |

### Important implementation detail

Create, update, and delete are implemented in the application, but their persistent storage is the database through Spring Data JPA. The Java array is used for listing and linear search; it is not the persistent CRUD store. Search is available through `GET /api/bins?search=<text>`.

## Task 2: Singly Linked List

### Structure and operations

The project defines its own generic singly linked list in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java).

| Linked-list concept | Implementation location |
|---|---|
| Head, tail, and size | [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L11-L14) |
| Node containing a value and next reference | [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L80-L86) |
| Insert at tail | `add` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L16-L25) |
| Find/search | `find` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L27-L34) |
| Update | `update` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L36-L44) |
| Delete | `remove` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L46-L66) |
| Traverse/display in list order | `toList` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L68-L74) |
| Get element count | `size` in [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L76-L78) |

### Integration in WasteFlow

During route planning, selected collection requests are ordered by priority and added to the linked list. The route service searches for duplicate requests, converts the list to ordered requests, updates each request to assigned, removes it from the working list, and appends the corresponding route stop to another linked list. It then traverses the stop list to persist stops in order. See [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L51-L68) and [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L86-L99).

A linked list is suitable here because the number of selected requests and route stops varies per route. The structure can grow dynamically and appending at the tail is constant time. Searching, updating, and removing by value require traversal, which is acceptable for the small route lists in this project.

## Tests

The linked-list operations are covered by [SinglyLinkedListTest.java](backend/src/test/java/com/wasteflow/route/SinglyLinkedListTest.java). Linear bin search is covered by [WasteBinServiceTest.java](backend/src/test/java/com/wasteflow/bin/WasteBinServiceTest.java).

## Summary

Task 1 concepts are used for bin display and search, while the application's create, update, and delete operations persist through the database. Task 2's custom singly linked list is integrated into route planning and provides insert, find, update, delete, traversal, and size operations.
