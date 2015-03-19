package team302;

import battlecode.common.*;

public class HELIPAD {
    public static RobotController rc;
    public static RobotType[] canSpawn = {RobotType.DRONE};
    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
        Util.spawnWithPrecedence(rc, Direction.NORTH, canSpawn);
    }
}