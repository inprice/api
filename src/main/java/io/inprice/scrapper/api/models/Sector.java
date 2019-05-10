package io.inprice.scrapper.api.models;

public class Sector extends Model {

    private String name;

    public Sector() {
    }

    public Sector(Long id, String name) {
        setId(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Sector{" +
                "name='" + name + '\'' +
                '}';
    }
}
