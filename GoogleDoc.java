import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GoogleDoc {

    public static void main(String[] args) {
        Document document = new Document(new ArrayList<>());
        DocumentEditor editor = new DocumentEditor(document, new FilePersistentStorage());

        editor.addText("Line 1 — first text");
        editor.addText("Line 2 — second text");
        editor.addImage("/assets/photo.png");
        editor.addText("Line 4 — after image");
        editor.addText("Line 5 — last text");

        System.out.println("=== All 5 elements (0-based index: 0..4) ===");
        editor.renderDocument();

        if (document.getElementAt(2) instanceof ImageDocumentElement) {
            System.out.println("\n=== Deleting 3rd element (index 2), image ===");
            editor.removeAt(2);
        }

        System.out.println("\n=== After delete: 4 elements, order preserved ===");
        editor.renderDocument();

        System.out.println("\n=== Replacing text at index 1 with new string ===");
        if (editor.updateTextAt(1, "Line 2 — [UPDATED] replacement text")) {
            editor.renderDocument();
        }

        System.out.println("\n=== Undo: revert text update at index 1 ===");
        editor.undo();
        editor.renderDocument();

        System.out.println("\n=== Undo: re-insert image at index 2 (5 elements) ===");
        editor.undo();
        editor.renderDocument();

        System.out.println("\n=== Redo: remove image again (4 elements) ===");
        editor.redo();
        editor.renderDocument();

        editor.saveDocument();
    }
}

interface DocumentElement {
    void render();
}

interface PersistentStorage {
    boolean save(Document document);
}

class FilePersistentStorage implements PersistentStorage {
    @Override
    public boolean save(Document document) {
        if (document == null) { System.out.println("Document is null"); return false; }
        System.out.println("Saving document in file");
        return true;
    }
}

class DBPersistentStorage implements PersistentStorage {
    @Override
    public boolean save(Document document) {
        if (document == null) { System.out.println("Document is null"); return false; }
        System.out.println("Saving document in DB");
        return true;
    }
}

class TextDocumentElement implements DocumentElement {
    private final String content;

    TextDocumentElement(String content) { this.content = content; }

    @Override
    public void render() {
        System.out.println("This is a TextDocumentElement");
        System.out.println("Text: " + content);
    }
}

class ImageDocumentElement implements DocumentElement {
    private final String path;

    ImageDocumentElement(String path) { this.path = path; }

    @Override
    public void render() {
        System.out.println("This is a ImageDocumentElement");
        System.out.println("Image path: " + path);
    }
}

class Document {
    List<DocumentElement> elements;

    Document(List<DocumentElement> elements) { this.elements = elements; }

    public List<DocumentElement> getElements() { return elements; }

    public void addElement(DocumentElement element) { elements.add(element); }

    public void addElementAt(int index, DocumentElement element) { elements.add(index, element); }

    public DocumentElement getElementAt(int index) { return elements.get(index); }

    public void removeAt(int index) { elements.remove(index); }

    public void replaceElementAt(int index, DocumentElement element) { elements.set(index, element); }
}

interface EditCommand {
    void undo();
    void redo();
}

final class AddStackElementCommand implements EditCommand {
    private final Document document;
    private final int index;
    private final DocumentElement element;

    AddStackElementCommand(Document document, int index, DocumentElement element) {
        this.document = document;
        this.index = index;
        this.element = element;
    }

    @Override
    public void undo() { document.removeAt(index); }

    @Override
    public void redo() { document.addElementAt(index, element); }
}

final class RemoveStackElementCommand implements EditCommand {
    private final Document document;
    private final int index;
    private final DocumentElement removed;

    RemoveStackElementCommand(Document document, int index, DocumentElement removed) {
        this.document = document;
        this.index = index;
        this.removed = removed;
    }

    @Override
    public void undo() { document.addElementAt(index, removed); }

    @Override
    public void redo() { document.removeAt(index); }
}

final class ReplaceElementCommand implements EditCommand {
    private final Document document;
    private final int index;
    private final DocumentElement before;
    private final DocumentElement after;

    ReplaceElementCommand(Document document, int index, DocumentElement before, DocumentElement after) {
        this.document = document;
        this.index = index;
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() { document.replaceElementAt(index, before); }

    @Override
    public void redo() { document.replaceElementAt(index, after); }
}

class DocumentEditor {
    private final Document doc;
    private final PersistentStorage storage;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();

    DocumentEditor(Document doc, PersistentStorage storage) {
        this.doc = doc;
        this.storage = storage;
    }

    public void addText(String text) {
        int index = doc.getElements().size();
        DocumentElement element = new TextDocumentElement(text);
        doc.addElement(element);
        recordEdit(new AddStackElementCommand(doc, index, element));
    }

    public void addImage(String path) {
        int index = doc.getElements().size();
        DocumentElement element = new ImageDocumentElement(path);
        doc.addElement(element);
        recordEdit(new AddStackElementCommand(doc, index, element));
    }

    public boolean saveDocument() { return storage.save(doc); }

    public void removeAt(int index) {
        DocumentElement removed = doc.getElementAt(index);
        doc.removeAt(index);
        recordEdit(new RemoveStackElementCommand(doc, index, removed));
    }

    public void replaceElementAt(int index, DocumentElement element) {
        DocumentElement before = doc.getElementAt(index);
        doc.replaceElementAt(index, element);
        recordEdit(new ReplaceElementCommand(doc, index, before, element));
    }

    public boolean updateTextAt(int index, String newText) {
        if (!(doc.getElementAt(index) instanceof TextDocumentElement)) return false;
        DocumentElement before = doc.getElementAt(index);
        DocumentElement after = new TextDocumentElement(newText);
        doc.replaceElementAt(index, after);
        recordEdit(new ReplaceElementCommand(doc, index, before, after));
        return true;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        EditCommand command = undoStack.removeLast();
        command.undo();
        redoStack.addLast(command);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        EditCommand command = redoStack.removeLast();
        command.redo();
        undoStack.addLast(command);
        return true;
    }

    public void renderDocument() {
        for (DocumentElement documentElement : this.doc.getElements()) {
            documentElement.render();
        }
    }

    private void recordEdit(EditCommand command) {
        redoStack.clear();
        undoStack.addLast(command);
    }
}