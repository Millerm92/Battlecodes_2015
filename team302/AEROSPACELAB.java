package team302;

import battlecode.common.*;
import java.util.*;

public class AEROSPACELAB {

    public static RobotType[] canSpawn = {RobotType.LAUNCHER};

    public static void execute(RobotController rc_in) throws GameActionException {
        RobotController rc = rc_in;

        Util.spawnWithPrecedence(rc, Direction.NORTH, canSpawn);
    }
}