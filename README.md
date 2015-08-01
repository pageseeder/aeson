# Aeson

Aeson generates JSON out of a simple XML format.

## Aeson's approach

Aeson takes a different approach to generate JSON from XML, it defines an XML format
to represent JSON structures.

Most XML to JSON tools try to convert XML to JSON by defining complex transformation rules.
We found that unsuitable in our case, because it implies that XML and JSON are somehow interchangeable.
But this is rarely the case, both have different data structures suitable for different use cases.

Since they are already plenty of tools to convert XML to XML, we preferred concentrating on
defining an XML format that is both simple and natural to represent JSON data structures.

## Aeson format

The default Aeson conversion rules:

 * XML elements -> JSON objects
 * XML attributes -> JSON properties
 * `<json:array>` -> JSON arrays
 
The name of document element as well as any other type of node is ignored (comments, processing instructions, 
namespace declarations and nodes, text)

For example:

```
<root version="1.0">
  <person id="4" name="Ali Baba"/>
</root>
```

Will be serialized as

```
{
  "version": "1.0",
  "person": {
    "id": "4",
    "name": "Ali Baba"
  }
}
```

### Arrays

To create arrays, use the <json:array> element.

For example:

```
<json:array xmlns:json="http://pageseeder.org/JSON">
  <person id="1" name="Bob"/>
  <person id="2" name="Alice"/>
</json:array>
```

Will become:

```
[
  {"id": "1", "name": "Bob"},
  {"id": "2", "name": "Alice"}
]
```

For details, see <http://www.pageseeder.org/projects/aeson.html>

## Berlioz

Since version 0.9.33, Berlioz can automatically generate JSON from XML using the Aeson syntax.
