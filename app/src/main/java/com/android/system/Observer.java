package com.android.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Observer {
    private Map<String,List<EventHandler>> mHandlerListMap = new HashMap<>();

    public interface EventHandler {
        /**
         * override: handleEvent
         * @param event
         * @return: true, send next; false, stop;
         */
        boolean handleEvent(Object event);
    }

    void addEventHandler(String eventName, EventHandler handler) {
        List<EventHandler> handlerList = mHandlerListMap.get(eventName);
        if(handlerList == null) {
            mHandlerListMap.put(eventName, (handlerList = new ArrayList<>()) );
        }
        handlerList.add(handler);
    }

    void sendEvent(String eventName, Object event) {
        List<EventHandler> handlerList = mHandlerListMap.get(eventName);
        if(handlerList != null) {
            for(EventHandler handler: handlerList) {
                if(!handler.handleEvent(event)) {
                    break;
                }
            }
        }
    }

    void removeAllEventHandler() {
        mHandlerListMap.clear();
    }

    void removeEvent(String eventName) {
        mHandlerListMap.remove(eventName);
    }

    void removeEventHandler(String eventName, EventHandler handler) {
        List<EventHandler> handlerList = mHandlerListMap.get(eventName);
        if(handlerList != null) {
            handlerList.remove(handler);
        }
    }
}
