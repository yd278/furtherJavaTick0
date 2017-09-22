package uk.ac.cam.yd278.fjava.tick0;


import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalSort {
    static final int BLOCK_SIZE = 65536;

    public static void copy(DataInputStream dis, DataOutputStream dos, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            int a = dis.readInt();
            dos.writeInt(a);

        }
        dos.flush();
    }

    public static void output(List<Integer> block, DataOutputStream dos) throws IOException {
        for (Integer i : block) {
            dos.writeInt(i);
        }
        dos.flush();
    }

    public static void preProcess(DataInputStream dis, DataOutputStream dos, int length) throws FileNotFoundException, IOException {
        List<Integer> block = new ArrayList<>();
        int index = 0;
        long counter = 0;
        while (counter < length) {
            block.add(dis.readInt());
            index++;
            counter++;
            if (index == BLOCK_SIZE || counter == length) {
                Collections.sort(block);
                output(block, dos);
                index = 0;
                block = new ArrayList<>();
            }
        }

    }

    public static void merge(
            DataInputStream dis1, DataInputStream dis2, DataOutputStream dos, int length1, int length2) throws IOException {
        int a =dis1.readInt();
        int b =dis2.readInt();
        int count1 = 0;
        int count2 = 0;

        while (count1 < length1 && count2 < length2) {
            if (a < b) {
                dos.writeInt(a);
                count1++;
                a = dis1.readInt();
            } else {
                dos.writeInt(b);
                count2++;
                if(count2 == length2)break;
                b = dis2.readInt();
            }


        }
        dos.flush();
        if (count1 == length1) {;
            dos.writeInt(b);
            copy(dis2, dos, length2 - count2 - 1);
        } else {
            dos.writeInt(a);
            copy(dis1, dos, length1 - count1 - 1);

        }

    }

    public static void sort(String f1, String f2) throws IOException {
        RandomAccessFile a = new RandomAccessFile(f1, "rw");
        RandomAccessFile b = new RandomAccessFile(f2, "rw");
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(a.getFD())));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(b.getFD())));
        int length = (int) a.length() / 4;
        preProcess(dis, dos, length);

        int currentBlockSize = BLOCK_SIZE;
        String from = f2;
        String to = f1;
        while (currentBlockSize < length) {
            int start = 0;
            int end = 2 * currentBlockSize;
            DataOutputStream dos2 = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new RandomAccessFile(to,"rw").getFD()))
            );
            while (end < length) {
                DataInputStream dis1 = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
                );
                dis1.skipBytes(start * 4);
                DataInputStream dis2 = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
                );
                dis2.skipBytes((start + currentBlockSize) * 4);
                merge(dis1, dis2, dos2, currentBlockSize, currentBlockSize);
                start = end;
                end = start + 2 * currentBlockSize;
            }
            if (start + currentBlockSize >= length) {
                DataInputStream dis1 = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
                );
                dis1.skipBytes(start * 4);
                copy(dis1, dos2, length - start);
            }else{
                DataInputStream dis1 = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
                );
                dis1.skipBytes(start * 4);
                DataInputStream dis2 = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
                );
                dis2.skipBytes((start + currentBlockSize) * 4);
                merge(dis1,dis2,dos2,currentBlockSize,length - start - currentBlockSize);
            }



            String tmp = from;
            from = to;
            to = tmp;
            currentBlockSize *= 2;
        }
        if(from == f2){
            DataInputStream dis1 = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(new RandomAccessFile(from,"rw").getFD()))
            );
            DataOutputStream dos2 = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(new RandomAccessFile(to,"rw").getFD()))
            );
            copy(dis1,dos2,length);
        }
    }

    private static String byteToHex(byte b) {
        String r = Integer.toHexString(b);
        if (r.length() == 8) {
            return r.substring(6);
        }
        return r;
    }

    public static String checkSum(String f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream ds = new DigestInputStream(
                    new FileInputStream(f), md);
            byte[] b = new byte[512];
            while (ds.read(b) != -1)
                ;

            String computed = "";
            for (byte v : md.digest())
                computed += byteToHex(v);

            return computed;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "<error computing checksum>";
    }

    public static void main(String[] args) throws Exception {
        String f1 = args[0];
        String f2 = args[1];
        sort(f1, f2);
        System.out.println("The checksum is: " + checkSum(f1));
    }
}
