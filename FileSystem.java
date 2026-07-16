import java.util.*;

/*
Design In memory file system and implement:
Create - done
Delete - done
Move - done
List all children - done
Search - done
* */
public class FileSystem {
    public static void main(String[] args) {
        FileNode root = new FileNode(FileNodeType.DIRECTORY, "");
        FileNode one = new FileNode(FileNodeType.FILE, "1");
        FileNode two = new FileNode(FileNodeType.DIRECTORY, "2");
        FileNode three = new FileNode(FileNodeType.FILE, "3");
        FileNode four = new FileNode(FileNodeType.DIRECTORY, "4");
        FileNode five = new FileNode(FileNodeType.FILE, "t1.txt");
        FileNode six = new FileNode(FileNodeType.DIRECTORY, "6");
        FileNode seven = new FileNode(FileNodeType.FILE, "7");
        FileNode eight = new FileNode(FileNodeType.FILE, "t1.txt");
        FileNode nine = new FileNode(FileNodeType.FILE, "9");
        FileNode ten = new FileNode(FileNodeType.FILE, "t1.txt");

        root.addChild(one);
        root.addChild(two);
        root.addChild(three);
        root.addChild(four);

        two.addChild(five);
        two.addChild(six);
        two.addChild(seven);

        six.addChild(eight);
        six.addChild(nine);

        four.addChild(ten);

        List<FileNode> fileNodes = new ArrayList<>();
        root.search(new String[]{"*", "*", "t1.txt"}, root, 0, fileNodes);
        System.out.println(fileNodes.size());

        root.printFileSystem(root);
    }
}

enum FileNodeType {
    DIRECTORY,
    FILE
}

class FileNode {
    static FileNode root;
    Set<FileNode> children;
    HashMap<String, FileNode> childMap;
    FileNodeType type;
    String name;

    public FileNode(FileNodeType fileNodeType, String name) {
        if (fileNodeType == FileNodeType.DIRECTORY) {
            children = new HashSet<>();
            childMap = new HashMap<>();
        }
        type = fileNodeType;
        this.name = name;
        if (root == null) {
            root = this;
        }
    }

    public boolean addChild(FileNode fileNode) {
        if (this.type != FileNodeType.DIRECTORY) {
            return false;
        }
        children.add(fileNode);
        childMap.put(fileNode.name, fileNode);
        return true;
    }

    public boolean removeChild(FileNode fileNode) {
        if (this.type != FileNodeType.DIRECTORY) {
            return false;
        }
        children.remove(fileNode);
        childMap.remove(fileNode.name);
        return true;
    }

    public FileNode getFileNode(String path) {
        FileNode curr = root;
        String[] toPathArr = path.split("/");
        System.out.println("Path " + path);
        System.out.println(toPathArr.length);
        int i = 0;

        while (i < toPathArr.length) {
            if (curr.childMap.containsKey(toPathArr[i])) {
                curr = curr.childMap.get(toPathArr[i]);
            } else {
                return null;
            }
            i++;
        }
        return curr;
    }

    public boolean move(String fromPath, String toPath) {
        FileNode toPathNode = getFileNode(toPath);

        int idx = fromPath.lastIndexOf("/");
        String fromPathDir = fromPath.substring(0, idx);
        String fromPathFileName = fromPath.substring(idx);
        FileNode fromPathNode = getFileNode(fromPath);

        if (toPathNode == null || fromPathNode == null) {
            return false;
        }

        FileNode fileNode = fromPathNode.childMap.get(fromPathFileName);
        toPathNode.addChild(fileNode);
        fromPathNode.removeChild(fileNode);
        return true;
    }

    public void listChildren(String path) {
        if (this.type != FileNodeType.DIRECTORY) {
            return;
        }
        System.out.println("CHILDREN");
        FileNode fileNode = getFileNode(path);
        if (fileNode == null) {
            return;
        }
        for (FileNode fn : fileNode.children) {
            System.out.println(fn.name);
        }
    }

    public void search(String[] toPathArr, FileNode root, int idx, List<FileNode> result) {
        if (idx == toPathArr.length) {
            result.add(root);
            return;
        }

        if (root.type == FileNodeType.FILE) {
            return;
        }

        if (toPathArr[idx].equals("*")) {
            for (FileNode fn : root.children) {
                search(toPathArr, fn, idx + 1, result);
            }
        }

        FileNode next = root.childMap.get(toPathArr[idx]);
        if (next != null) {
            search(toPathArr, next, idx + 1, result);
        }
    }

    public void printFileSystem(FileNode curr) {
        for (FileNode fn : curr.children) {
            System.out.println(curr.name + " " + fn.name);
        }
        for (FileNode fn : curr.children) {
            if (fn.type == FileNodeType.DIRECTORY) {
                printFileSystem(fn);
            }
        }
    }

}