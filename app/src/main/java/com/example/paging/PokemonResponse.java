package com.example.paging;

public class PokemonResponse {
    public Species species;
    public Sprites sprites;

    public static class Species{
        public String name;
    }

    public static class Sprites{
        public String frontDefault;
    }
}
