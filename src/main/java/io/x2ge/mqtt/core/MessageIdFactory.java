package io.x2ge.mqtt.core;

import java.util.Hashtable;

public class MessageIdFactory {

    private static final Hashtable<Integer, Integer> using = new Hashtable<>();
    private static int lastId = 0;

    public static int get() throws Exception {
        synchronized (using) {
            int id = lastId;
            for (int i = 1; i <= 65535; i++) {
                // id范围1~65535
                ++id;
                if (id < 1 || id > 65535)
                    id = 1;

                if (!using.contains(id)) {
                    using.put(id, id);
                    lastId = id;
                    return id;
                }
            }
            throw new Exception("The message id has been used up!");
        }
    }

    public static void release(int id) {
        if (id > 0)
            synchronized (using) {
                using.remove(id);
            }
    }
}
