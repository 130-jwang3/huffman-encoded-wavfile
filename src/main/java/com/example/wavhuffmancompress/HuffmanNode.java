package com.example.wavhuffmancompress;

public class HuffmanNode implements Comparable<HuffmanNode> {
    int data;
    int frequency;
    HuffmanNode left, right;

    public HuffmanNode(int data, int frequency) {
        this.data = data;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        return this.frequency - other.frequency;
    }
}