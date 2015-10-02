package org.opendatanode.plugins.extractor.sk.judge;

public class Judge {
    
    private boolean active;
    private String name;
    private String function;
    private String location;
    private String note;
    
    public Judge(boolean active, String name, String function, String location, String note) throws Exception {
        this.active = active;
        this.name = name;
        this.function = function;
        this.location = location;
        this.note = note;
        
        if (name == null || name.isEmpty()) {
            throw new Exception("Name not found: " + this.toString());
        }
        
//        if (function == null || function.isEmpty()) {
//            throw new Exception("Function not found" + this.toString());
//        }
//        
//        if (location == null || location.isEmpty()) {
//            throw new Exception("Location not found" + this.toString());
//        }
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }

    public String getFunction() {
        return function;
    }

    public String getLocation() {
        return location;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "Judge [active=" + active + ", name=" + name + ", function=" + function + ", location=" + location + ", note=" + note + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (active ? 1231 : 1237);
        result = prime * result + ((function == null) ? 0 : function.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((note == null) ? 0 : note.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Judge other = (Judge) obj;
        if (active != other.active)
            return false;
        if (function == null) {
            if (other.function != null)
                return false;
        } else if (!function.equals(other.function))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (note == null) {
            if (other.note != null)
                return false;
        } else if (!note.equals(other.note))
            return false;
        return true;
    }
}
