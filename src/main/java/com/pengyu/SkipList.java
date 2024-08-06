package main.java.com.pengyu;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * 跳表
 * @param <K>
 * @param <V>
 */
public class SkipList <K extends  Comparable<K>, V>{

    /**
     * 节点 内部类
     * @param <K>
     * @param <V>
     */
    private static class Node<K extends  Comparable<K>, V> {
        K key; // key
        V value; // value
        int level; // 层级
        ArrayList<Node<K, V>> forwards; // 节点每层的下一跳指向

        Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            this.level = level;
            this.forwards = new ArrayList<>(Collections.nCopies(MAX_LEVEL + 1, null));
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    // 跳表最大高度
    private static final int MAX_LEVEL = 32;

    // 头节点
    private Node<K, V> head;

    // 节点个数
    private int nodeCnt;

    // 当前最高层级
    private int curLevel;

    // 跳表持久化路径
    private static String STORE_PATH = "./store";

    // 跳表构造方法
    SkipList() {
        this.head = new Node<>(null, null, MAX_LEVEL);
        this.nodeCnt = 0;
        this.curLevel = 0;
    }

    /**
     * 随机算法生成层级
     */
    private static int getRandomLevel() {
        int level = 1;
        Random random = new Random();
        // 层级概率
        /**
         * 2 1/2
         * 3 1/4
         * 4 1/8
         * 5 1/16
         */
        while (random.nextInt(2) == 1) {
            level++;
            if (level == MAX_LEVEL) break;
        }
        return level;
    }


    /**
     * 插入
     */
    public synchronized boolean add(K key, V value) {
        Node<K, V> curNode = this.head;

        ArrayList<Node<K, V>> preNodes = new ArrayList<>(Collections.nCopies(MAX_LEVEL + 1, null));
        for (int i = curLevel; i >= 0; i--) {
            while (curNode.forwards.get(i) != null && curNode.forwards.get(i).getKey().compareTo(key) < 0) {
                curNode = curNode.forwards.get(i);
            }
            preNodes.set(i, curNode);
        }

        curNode = curNode.forwards.get(0);

        // 如果key已经存在
        if (curNode != null && curNode.getKey().compareTo(key) == 0) {
            curNode.setValue(value);
            return true;
        }

        // 随机节点层数
        int randomLevel = getRandomLevel();

        if (curNode == null || curNode.getKey().compareTo(key) != 0) {
            if (randomLevel > curLevel) {
                for (int i = curLevel + 1; i < randomLevel + 1; i++) {
                    preNodes.set(i, head);
                }
                // 更新跳表高度
                curLevel = randomLevel;
            }
            Node<K, V> insertNode = new Node<>(key, value, randomLevel);

            for (int i = 0; i <= randomLevel; i++) {
                insertNode.forwards.set(i, preNodes.get(i).forwards.get(i));
                preNodes.get(i).forwards.set(i, insertNode);
            }
            nodeCnt++;
            return true;
        }
        return false;
    }

    /**
     * 获取key对应的value值
     * @param key
     * @return
     */
    public V get(K key) {

        Node<K, V> curNode = this.head;

        for (int i = curLevel; i >= 0; i--) {
            while (curNode.forwards.get(i) != null && curNode.forwards.get(i).getKey().compareTo(key) < 0) {
                curNode = curNode.forwards.get(i);
            }
        }

        curNode = curNode.forwards.get(0);
        if (curNode != null && curNode.getKey().compareTo(key) == 0) {
            return curNode.getValue();
        }
        return null;
    }

    /**
     * 删除节点
     * @return
     */
    public synchronized boolean del(K key) {
        Node<K, V> curNode = this.head;

        ArrayList<Node<K, V>> preNodes = new ArrayList<>(Collections.nCopies(MAX_LEVEL + 1, null));

        for (int i = curLevel; i >= 0; i--) {
            while (curNode.forwards.get(i) != null && curNode.forwards.get(i).getKey().compareTo(key) < 0) {
                curNode = curNode.forwards.get(i);
            }
            preNodes.set(i, curNode);
        }

        curNode = curNode.forwards.get(0);

        // 如果key已经存在
        if (curNode != null && curNode.getKey().compareTo(key) == 0) {
            for (int i = 0; i < this.curLevel; i++) {
                if (preNodes.get(i).forwards.get(i) != curNode) break;

                preNodes.get(i).forwards.set(i, curNode.forwards.get(i));
            }
        }

        // 删除后若上层不存在节点，高度减小
        while (this.curLevel > 0 && this.head.forwards.get(this.curLevel) == null) {
            this.curLevel--;
        }
        this.nodeCnt--;
        return true;
    }

    /**
     * 查看key是否存在
     * @param key
     * @return
     */
    public boolean searchNode(K key) {
        Node<K, V> current = this.head;

        for (int i = this.curLevel; i >= 0; i--) {
            while (current.forwards.get(i) != null && current.forwards.get(i).getKey().compareTo(key) < 0) {
                current = current.forwards.get(i);
            }
        }

        current = current.forwards.get(0);
        return current != null && current.getKey().compareTo(key) == 0;
    }

    /**
     * 持久化跳表内的数据
     */
    public void dumpFile() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(STORE_PATH))) {
            Node<K, V> node = this.head.forwards.get(0);
            while (node != null) {
                String data = node.getKey() + ":" + node.getValue() + ";";
                bufferedWriter.write(data);
                bufferedWriter.newLine();
                node = node.forwards.get(0);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump file", e);
        }
    }

    /**
     * 从文本文件中读取数据
     */
    public void loadFile() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(STORE_PATH))) {
            String data;
            while ((data = bufferedReader.readLine()) != null) {
                System.out.println(data);
                Node<K, V> node = getKeyValueFromString(data);
                if (node != null) {
                    add(node.getKey(), node.getValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断读取的字符串是否合法
     *
     * @param data 字符串
     * @return 合法返回 true，非法返回 false
     */
    private boolean isValidString(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        if (!data.contains(":")) {
            return false;
        }
        return true;
    }

    /**
     * 根据文件中的持久化字符串，获取 key 和 value，并将 key 和 value 封装到 Node 对象中
     * @param data 字符串
     * @return 返回该字符串对应的key和value 组成的 Node 实例，如果字符串非法，则返回 null
     */
    private Node<K, V> getKeyValueFromString(String data) {
        if (!isValidString(data)) return null;
        String substring = data.substring(0, data.indexOf(":"));
        K key = (K) substring;
        // 去掉分号，不要结尾冒号
        String substring1 = data.substring(data.indexOf(":") + 1, data.length() - 1);
        V value = (V) substring1;
        return new Node<K, V>(key, value, 1);
    }

    /**
     * 打印跳表的结构
     */
    public void displaySkipList() {
        // 从最上层开始向下遍历所有层
        for (int i = this.curLevel; i >= 0; i--) {
            Node<K, V> node = this.head.forwards.get(i);
            System.out.print("Level " + i + ": ");
            // 遍历当前层的所有节点
            while (node != null) {
                // 打印当前节点的键和值，键值对之间用":"分隔
                System.out.print(node.getKey() + ":" + node.getValue() + ";");
                // 移动到当前层的下一个节点
                node = node.forwards.get(i);
            }
            // 当前层遍历结束，换行
            System.out.println();
        }
    }

    public static void main(String[] args) {
        SkipList<String, String> skipList = new SkipList<>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String command = scanner.nextLine();
            String[] commandList = command.split(" ");
            if (commandList[0].equals("add")) {
                boolean b = skipList.add(commandList[1], commandList[2]);
                if (b) {
                    System.out.println("Key: " + commandList[1] + " Value: " + commandList[2] + " insert success!");
                } else {
                    System.out.println("Key: " + commandList[1] + " Value: " + commandList[2] + " insert failed");
                }
            } else if (commandList[0].equals("del")) {
                boolean b = skipList.del(commandList[1]);
                if (b) {
                    System.out.println("Key: " + commandList[1] + " deleted!");
                } else {
                    System.out.println("skiplist not exists the key: " + commandList[1]);
                }
            } else if (commandList[0].equals("search")) {
                boolean b = skipList.searchNode(commandList[1]);
                if (b) {
                    System.out.println("Key: " + commandList[1] + "  exists!");
                } else {
                    System.out.println("Key: " + commandList[1] + " not exists!");
                }
            } else if (commandList[0].equals("get")) {
                if (!skipList.searchNode(commandList[1])) {
                    System.out.println("Key: " + commandList[1] + " not exists!");
                }
                String node = skipList.get(commandList[1]);
                if (node != null) {
                    System.out.println("Key: " + commandList[1] + "'s value is " + node);
                }
            } else if (commandList[0].equals("dump")) {
                skipList.dumpFile();
                System.out.println("Already saved skiplist.");
            } else if (commandList[0].equals("load")) {
                skipList.loadFile();
            } else {
                System.out.println("********skiplist*********");
                skipList.displaySkipList();
                System.out.println("*************************");
            }
        }
    }
}
