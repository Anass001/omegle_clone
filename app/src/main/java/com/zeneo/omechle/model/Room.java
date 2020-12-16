package com.zeneo.omechle.model;

import java.io.Serializable;
import java.util.List;

public class Room implements Serializable {

    private String id;

    private List<User> users;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
