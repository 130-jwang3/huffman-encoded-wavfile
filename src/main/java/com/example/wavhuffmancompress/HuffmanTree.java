package com.example.wavhuffmancompress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public class HuffmanTree {
    public static HashMap<Integer, String> buildHuffmanCodes(int[] data) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        // Calculate byte frequencies
        for (int b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }

        PriorityQueue<HuffmanNode> minHeap = new PriorityQueue<>();

        // Create a priority queue of Huffman nodes
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            minHeap.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        // Build the Huffman tree
        while (minHeap.size() > 1) {
            HuffmanNode left = minHeap.poll();
            HuffmanNode right = minHeap.poll();
            HuffmanNode merged = new HuffmanNode((byte) 0, left.frequency + Objects.requireNonNull(right).frequency);
            merged.left = left;
            merged.right = right;
            minHeap.add(merged);
        }

        // Create Huffman codes
        HashMap<Integer, String> huffmanCodes = new HashMap<>();
        generateHuffmanCodes(minHeap.peek(), "", huffmanCodes);

        return huffmanCodes;
    }

    private static void generateHuffmanCodes(HuffmanNode node, String code, Map<Integer, String> huffmanCodes) {
        if (node == null) {
            return;
        }

        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.data, code);
        }

        generateHuffmanCodes(node.left, code + "0", huffmanCodes);
        generateHuffmanCodes(node.right, code + "1", huffmanCodes);
    }
}
