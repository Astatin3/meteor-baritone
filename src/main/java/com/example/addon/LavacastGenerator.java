package com.example.addon;

import java.util.ArrayList;
import java.util.List;

public class LavacastGenerator {

    public static class Position {
        public int x, z;

        public Position(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position p = (Position) o;
            return x == p.x && z == p.z;
        }

        public double distanceTo(Position other){
            return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(z - other.z, 2));
        }

        public Position IncPosX(){
            return new Position(x+1, z);
        }
        public Position IncPosZ(){
            return new Position(x, z+1);
        }
        public Position IncNegX(){
            return new Position(x-1, z);
        }
        public Position IncNegZ(){
            return new Position(x, z-1);
        }
    }

    public static class MovementNode {
        public Position position;
        public int depth;
        public int depthBeyond = 0;
        public MovementNode(Position pos, int depth){
            position = pos;
            this.depth = depth;
        }
        public MovementNode PosX = null;
        public MovementNode PosZ = null;
        public MovementNode NegX = null;
        public MovementNode NegZ = null;

        boolean hasExpanded = false;

        public void expand(List<Position> targetTiles, List<Position> visited) {
//        Set<Position>

            if(hasExpanded) {
                if(PosX != null) PosX.expand(targetTiles, visited);
                if(PosZ != null) PosZ.expand(targetTiles, visited);
                if(NegX != null) NegX.expand(targetTiles, visited);
                if(NegZ != null) NegZ.expand(targetTiles, visited);
                depthBeyond += 1;
                return;
            }

            Position IncPosX = position.IncPosX();
            if(!visited.contains(IncPosX) && ShouldMoveInDirection(targetTiles, visited, position, IncPosX)) {
                PosX = new MovementNode(IncPosX, depth + 1);
                visited.add(IncPosX);
            }

            Position IncPosZ = position.IncPosZ();
            if(!visited.contains(IncPosZ) && ShouldMoveInDirection(targetTiles, visited, position, IncPosZ)) {
                PosZ = new MovementNode(IncPosZ, depth + 1);
                visited.add(IncPosZ);
            }

            Position IncNegX = position.IncNegX();
            if(!visited.contains(IncNegX) && ShouldMoveInDirection(targetTiles, visited, position, IncNegX)) {
                NegX = new MovementNode(IncNegX, depth + 1);
                visited.add(IncNegX);
            }

            Position IncNegZ = position.IncNegZ();
            if(!visited.contains(IncNegZ) && ShouldMoveInDirection(targetTiles, visited, position, IncNegZ)) {
                NegZ = new MovementNode(IncNegZ, depth + 1);
                visited.add(IncNegZ);
            }


            hasExpanded = true;
        }
    }

    private static boolean ShouldMoveInDirection(List<Position> targetTiles, List<Position> visited, Position origin, Position newPos){
        for(Position tile : targetTiles){
            if(visited.contains(tile)) continue;

            if(newPos.equals(tile)) return true;

            double distance = newPos.distanceTo(tile);

            if(distance > 2) continue;

            if(distance
                < origin.distanceTo(tile)) {
                return true;
            }

        }

        System.out.println("False");

        return false;
    }

//    public static MovementNode findSolution(List<Position> targetTiles, Position start) {
//
//
//
//        return root;
//    }

}
