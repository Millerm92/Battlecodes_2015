package team302;

import battlecode.common.*;

public class TANKFACTORY {
    public static RobotController rc;
    public static RobotType[] canSpawn = {RobotType.TANK};
    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;

        Util.spawnWithPrecedence(rc, Direction.NORTH, canSpawn);
    }
}