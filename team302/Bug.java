package team302;

import java.lang.System;
import java.util.ArrayDeque;
import java.util.ArrayList;
import battlecode.common.*;

public class Bug {
    static RobotController rc;
    static Direction allDirections[] = Direction.values();
    static MapLocation goal = null;
    static ArrayList<MapLocation> currentMLine = null;
    static MapLocation myLocation = null;
    static MapLocation lastLocation = null;
    static MapLocation wallStartLocation = null;
    static Direction myDirection = null;
    static boolean movedClockwise = false;
    static Direction waitToGo = null;

    public enum BugState {
        NO_MLINE,
        ON_MLINE,
        ON_WALL,
        AT_GOAL
    };

    private static BugState state = BugState.NO_MLINE;

    public static Direction startBuggin(RobotController rc_in,  MapLocation goal_in, int quitThresh) throws GameActionException {
        rc = rc_in;
        myLocation = rc.getLocation();

        //if i changed goals or am not where i left off last start over
        if ((goal == null || lastLocation == null) || !goal.equals(goal_in) || !lastLocation.equals(myLocation)) {
            freshstart();
            goal = goal_in;
        }

        //im at the goal yo!
        if(myLocation.distanceSquaredTo(goal) <= quitThresh) {
            state = BugState.AT_GOAL;
            return Direction.OMNI;
        }

        //if we are back at where we started on the wall
        if (wallStartLocation != null && myLocation.equals(wallStartLocation)){
            freshstart();
            return Direction.OMNI;
        }

        Direction dirToMove = bugNextMove();

        //if its omni we are following the map bounds so restart
        if (dirToMove == Direction.OMNI) {
            freshstart();
            return dirToMove;
        }

        lastLocation = myLocation.add(dirToMove);

        if(lastLocation.distanceSquaredTo(goal) <= quitThresh) {
            state = BugState.AT_GOAL;
            return Direction.OMNI;
        } else if (currentMLine.contains(lastLocation)) {
            state = BugState.ON_MLINE;
        }

        myDirection = dirToMove;
        //Util.debug(rc, "MOVING  " + dirToMove.toString());
        return dirToMove;
    }

    public static void freshstart() {
        state = BugState.NO_MLINE;
        wallStartLocation = null;
        myDirection = null;
        currentMLine = null;
        waitToGo = null;
    }

    public static Direction bugNextMove() throws GameActionException {
        switch (state) {
            case NO_MLINE:
                calcMLine();
            case ON_MLINE:
                return moveOnMLine();
            case ON_WALL:
                return followWall();
        }
        //System.out.println("HERERERE??????WHY?????????");
        return Direction.OMNI;
    }

    //get the next location on the mLine and try to move there
    public static Direction moveOnMLine() throws GameActionException {
        int myLocationIndex = currentMLine.indexOf(myLocation);
        //get the next location on the mLine and try to move there
        MapLocation nextLocation = currentMLine.get(myLocationIndex + 1);
        Direction nextLocationDir = myLocation.directionTo(nextLocation);
        myDirection = nextLocationDir;
        if (isTeammateAtSpot(nextLocationDir)) return Direction.NONE;
        if (canMove(nextLocationDir)) {
            return nextLocationDir;
        } else {
            return getHandOnWall();
        }
    }

    public static Direction getHandOnWall() throws GameActionException {
        Direction rightDir = myDirection;
        Direction leftDir = myDirection;
        while (true) {
            rightDir = rightDir.rotateRight();
            leftDir = leftDir.rotateLeft();

            if (canMove(rightDir)) {
//                Util.debug(rc, "ON WALL RIGHT");
                state = BugState.ON_WALL;
                movedClockwise = true;
                wallStartLocation = myLocation.add(rightDir);
                return rightDir;
            }

            if (canMove(leftDir)) {
//                Util.debug(rc, "ON WALL LFET");
                state = BugState.ON_WALL;
                movedClockwise = false;
                wallStartLocation = myLocation.add(rightDir);
                return leftDir;
            }
        }
    }

    public static Direction followWall() throws GameActionException {
        //if we can get back on the mline do it
        if (currentMLine.contains(myLocation.add(myDirection)) && canMove(myDirection)) {
//            Util.debug(rc, "BACK ON MLINE");
            state = BugState.ON_MLINE;
            return myDirection;
        }

        Direction backInwards = rotateInDir(myDirection, movedClockwise);
        backInwards = rotateInDir(backInwards, movedClockwise);
        if (canMove(backInwards)) {
            return backInwards;
        }
        backInwards = rotateInDir(myDirection, movedClockwise);
        if (canMove(backInwards)) {
            return backInwards;
        }


        if (rc.senseTerrainTile(rc.getLocation().add(backInwards)) == TerrainTile.OFF_MAP) return Direction.OMNI;

        if (canMove(myDirection)) {
            return myDirection;
        }
        int turns = 0;
        while (true) {
            turns++;
            myDirection = rotateInDir(myDirection, !movedClockwise);
            if (canMove(myDirection)) {
                return myDirection;
            }
        }
    }

    public static boolean canMove(Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) {
            return false;
        }

        int extraSplash = (RobotPlayer.enemyTowers.length > 4) ? 50 : 35;
        if (myLocation.add(dir).distanceSquaredTo(RobotPlayer.enemyHq) <= extraSplash) {
            return false;
        }
        for (MapLocation towerLoc: RobotPlayer.enemyTowers) {
            if (rc.getLocation().add(dir).distanceSquaredTo(towerLoc) <= 24 && !goal.equals(towerLoc)) {
                return false;
            }
        }

        return true;
    }

    public static Direction rotateInDir(Direction startDir, boolean rotateLeft) throws GameActionException {
        if (rotateLeft) {
            return startDir.rotateLeft();
        } else {
            return startDir.rotateRight();
        }
    }

    public static void calcMLine() throws GameActionException {
        Direction dirToGoal;
        ArrayList<MapLocation> mLine = new ArrayList<MapLocation>();
        MapLocation currentLocation = rc.getLocation();
        while (!currentLocation.equals(goal)) {
            if (Clock.getBytecodesLeft() < 400) {
                rc.yield();
            }
            mLine.add(currentLocation);
            dirToGoal = currentLocation.directionTo(goal);
            currentLocation = currentLocation.add(dirToGoal);
        }
        mLine.add(goal);

        currentMLine = mLine;
    }

}