# Fenced code blocks with PlantUML source code in Markdown code

## Example 1 - rendered

```plantuml
@startuml

    interface List<E> {
       add(E): boolean
       remove(int): E
       get(int): E
       isEmpty(): boolean
       size(): int 
    }
    
    abstract class AbstractList<E>
    class ArrayList<E>
    class LinkedList<E>
    
    List <|.. AbstractList
    AbstractList <|-- ArrayList
    AbstractList <|-- LinkedList

@enduml
```

## Example 2 - not rendered

```
@startuml

 title: Links in diagrams

 class FluentPreview [[../../plugin/src/net/certiv/fluentmark/views/FluentPreview.java]]
 interface List [[https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/List.html]]

 FluentPreview -> ViewPart

@enduml
```