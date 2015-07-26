package com.otognan.driverpete.android;


import java.util.HashSet;
import java.util.Set;


//{"id":null,"username":null,"expires":0,"roles":[]}
public class User  {

    private Long id;

    private String username;

    private long expires;

    private boolean accountExpired;

    private boolean accountLocked;

    private boolean credentialsExpired;

    private boolean accountEnabled;

    private Set<String> roles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public String toString() {
        return getClass().getSimpleName() + ": " + getUsername();
    }
}

