package com.shpp.p2p.cs.adavydenko.assignment15;

/**
 * This class describes a structure of a node. Each node has a meaningful or
 * a null-value for "value" variable and a value for byte frequency.
 * <p>
 * Node class implements Comparable interface so that a priority queue
 * used by this program can sort nodes by their byteFrequency values.
 */
public class Node implements Comparable<Node> {
    /**
     * The left-child-node of the current node.
     */
    private Node leftChild = null;

    /**
     * The right-child-node of the current node.
     */
    private Node rightChild = null;

    /**
     * The value of a particular byte from the inFile.
     */
    protected Byte value = null;

    /**
     * The number of bytes of a particular value in the inFile.
     */
    protected int byteFrequency;

    /**
     * Is a constructor method that creates a node with some byte value
     * and value of byte frequency provided as an arguments.
     *
     * @param value         is a particular byte value. E.g. 1, -45, 127 etc.
     * @param byteFrequency is this byte frequency that shall be saved
     *                      to the information describing this node.
     */
    public Node(byte value, int byteFrequency) {
        this.value = value;
        this.byteFrequency = byteFrequency;
    }

    /**
     * Is a constructor method that creates a node with byte value null
     * and some value of byte frequency provided as an argument.
     *
     * @param byteFrequency is this byte frequency that shall be saved
     *                      to the information describing this node.
     */
    public Node(int byteFrequency) {
        this.byteFrequency = byteFrequency;
    }

    /**
     * Creates a left child for a particular node. The node provided as an
     * argument becomes this left child.
     *
     * @param leftChild a node provided as an argument that shall become
     *                  the left child of a current node.
     */
    protected void setLeftChild(Node leftChild) {
        this.leftChild = leftChild;
    }

    /**
     * Creates a right child for a particular node. The node provided as an
     * argument becomes this right child.
     *
     * @param rightChild a node provided as an argument that shall become
     *                   the right child of a current node.
     */
    protected void setRightChild(Node rightChild) {
        this.rightChild = rightChild;
    }

    /**
     * Provides the left child of a particular node. If there is not any,
     * returns null.
     *
     * @return the left child-node of the current node. Null if there is
     * no such node.
     */
    protected Node getLeftChild() {
        return this.leftChild;
    }

    /**
     * Provides the right child of a particular node. If there is not any,
     * returns null.
     *
     * @return the right child-node of the current node. Null if there is
     * no such node.
     */
    protected Node getRightChild() {
        return this.rightChild;
    }

    /**
     * Prints the information about a particular node to console.
     * Namely, its value (the byte it stands for), the frequency of
     * this byte in the inFile as well as frequencies of the children-nodes.
     */
    public void displayNode() {
        String message;
        if (leftChild != null && rightChild != null) {
            message = "Node: " + "value = " + value + ", frequency = " + byteFrequency +
                    ", leftChild = " + leftChild.byteFrequency + ", rightChild = " + rightChild.byteFrequency;
        } else {
            message = "Node: " + "value = " + value + ", frequency = " + byteFrequency +
                    ", leftChild = null, rightChild = null";
        }
        System.out.println(message);
    }

    /**
     * Helps the priority queue to sort particular node by comparing each node`s
     * byteFrequency values. The nodes with the smallest values are placed in the
     * beginning of the priority queue. The nodes with the biggest values are placed
     * in the end of the queue.
     *
     * @param node is any node that is compared to an other node
     * @return if this method returns a negative numeric, the current object will be
     * positioned before the one provided as an argument. If the method returns a
     * positive numeric, the provided object will be positioned after the second object.
     * If the method returns zero, both objects are equal.
     */
    @Override
    public int compareTo(Node node) {
        return node.byteFrequency > this.byteFrequency ? -1 : 1;
    }
}
