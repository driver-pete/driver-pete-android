package com.otognan.driverpete.android;


public class Route {

    private Long id;

    private boolean directionAtoB;

    private Long startDate;

    private Long finishDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getDirectionAtoB() {
        return directionAtoB;
    }

    public void setDirectionAtoB(boolean directionAtoB) {
        this.directionAtoB = directionAtoB;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Long finishDate) {
        this.finishDate = finishDate;
    }

    public Long getDuration() {
        return this.getFinishDate() - this.getStartDate();
    }
}

