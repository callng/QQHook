package moe.ore.tars.support;

public class TarsStrutPropertyInfo {
    private boolean isRequire;
    private int order;
    private Object defaultValue;
    private String name;
    private Object stamp;
    private String comment;

    public boolean isRequire() {
        return isRequire;
    }

    public void setRequire(boolean isRequire) {
        this.isRequire = isRequire;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getStamp() {
        return stamp;
    }

    public void setStamp(Object stamp) {
        this.stamp = stamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
