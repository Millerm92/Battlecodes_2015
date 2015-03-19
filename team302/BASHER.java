package team302;

import battlecode.common.*;

public class BASHER {
    public static RobotController rc;
	public static int[] targetPriority = MyConstants.harassPriorities;

    public static void execute(RobotController rc_in) throws GameActionException {
        rc = rc_in;
    	if(Clock.getRoundNum() == 500) {
    		//System.out.println("Dom is anus pwn");
    	}
        rc = rc_in;
        Group.group(rc);
    }
}