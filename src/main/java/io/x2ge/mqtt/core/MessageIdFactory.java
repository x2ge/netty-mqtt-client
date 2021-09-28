package io.x2ge.mqtt.core;

import java.util.Hashtable;

public class MessageIdFactory {

    private static final Hashtable<Integer, Integer> using = new Hashtable<>();


    public static int get() throws Exception {

        int id = 0;
        synchronized (using) {
            // id范围1~65535
            for (int i = 1; i <= 65535; i++) {
                if (!using.contains(i)) {
                    id = i;
                    break;
                }
            }
            if (id == 0) {
                throw new Exception("The message id has been used up!");
            }
            using.put(id, id);
        }

        return id;
    }

    public static void release(int id) {
        if (id > 0)
            synchronized (using) {
                using.remove(id);
            }
    }
}
