package team302;
import battlecode.common.*;

import java.lang.Package;

public class FollowTheLeader {
    public static RobotController rc;
    public static MapLocation myLocation;
    public static boolean defendDistressedTowers;
    public static MapLocation goal;
    public static RobotType goalType;// = RobotType.TOWER;
    public static RobotInfo[] enemyRobots;

    public static Direction[] allDirections = Direction.values();
    public static enum RallyState {
        UNASSIGNED,
        MOVING_TO_RALLY_LOC,
        RALLYING_LEADER,
        SWARMING_GOAL,
        COLLAPSING_GOAL
    };
    public static RallyState state = RallyState.UNASSIGNED;
    public static int groupOffset;

    /*
     * decides what to do based on the robots current rally state and calls it
     * also decides when to switch states
     */
    public static void follow(RobotController rc_in, int groupOffset_in) throws GameActionException {
        rc = rc_in;

        myLocation = RobotPlayer.myLocation;
        groupOffset = groupOffset_in;
        enemyRobots = rc.senseNearbyRobots(35, RobotPlayer.enemyTeam);
        goalType = RobotType.values()[rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE)];
        Util.debug(rc, "MAKE A MOVE");
        makeMove();
    }

    private static void makeMove() throws GameActionException {
    	if (rc.isWeaponReady()){
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0){
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null){
					if (rc.canAttackLocation(target)){
						rc.attackLocation(target);
                        return;
					}
				}
			}
		}
		
        int x, y;
        switch (state) {
            case UNASSIGNED:
                //get the rally point
                if (rc.getSupplyLevel() > 0) {
                    state = RallyState.MOVING_TO_RALLY_LOC;
                    makeMove();
                } else {
                    Util.debug(rc, "MOVE TO SUPPLY");
                    moveToSupply();
                }
                break;
            case MOVING_TO_RALLY_LOC:

                //get rally goal
                x = rc.readBroadcast(groupOffset + GroupConstants.LEADER_LOCATION);
                y = rc.readBroadcast(groupOffset + GroupConstants.LEADER_LOCATION + 1);
                goal = new MapLocation(x, y);

                Util.debug(rc, "MOVE TO RALLY POINT" + goal.toString()  + " h" + myLocation.distanceSquaredTo(goal));
                //if we are within 4 units^2 to goal we are rallying
                if (myLocation.distanceSquaredTo(goal) <= 6) {
                    state = RallyState.RALLYING_LEADER;
                    makeMove();
                }else {
                    Util.debug(rc, "MOVE TO RALLY");
                    movingToRally();
                }
                break;
            case RALLYING_LEADER:
                Util.debug(rc, "RALLYING LEADER");
                //get rally goal
                x = rc.readBroadcast(groupOffset + GroupConstants.LEADER_LOCATION);
                y = rc.readBroadcast(groupOffset + GroupConstants.LEADER_LOCATION + 1);
                goal = new MapLocation(x, y);

                //if there is an attack location swarm it
                x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);

                if (x != 0 && y != 0) {
                    goal = new MapLocation(x,y);
                    state = RallyState.SWARMING_GOAL;
                    makeMove();
                    return;
                }

                rallying();
                break;
            case SWARMING_GOAL:
                Util.debug(rc, "MOVE TO SWARM");
                // if there is a temp attack location go there
                x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                if (x == 0 && y == 0) {
                    state = RallyState.RALLYING_LEADER;
                    makeMove();
                    return;
                }
                int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);
                goalType = RobotType.values()[goalTypeOrdinal];

                if (rc.readBroadcast(groupOffset + GroupConstants.COLLAPSE) == 1) {
                    state = RallyState.COLLAPSING_GOAL;
                    makeMove();
                    return;
                }
                swarming();
                GroupUtils.updateSwarmCount(rc, goal, goalType, groupOffset);
                break;
            case COLLAPSING_GOAL:
                Util.debug(rc, "COLLAPSING");

                x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                if (x == 0 && y == 0) {
                    state = RallyState.RALLYING_LEADER;
                    makeMove();
                    return;
                }

                if (!collapsing()) {
                    state = RallyState.RALLYING_LEADER;
                    makeMove();
                    return;
                }
                break;
        }
    }

    /*
     * Moves to hq for supply
     */
    private static void moveToSupply() throws GameActionException {
        if (rc.isCoreReady()) {
            if (myLocation.distanceSquaredTo(RobotPlayer.myHq) <= 15) {
                if (RobotPlayer.type == RobotType.TANK) {
                    Util.requestSupply(rc, 12000, myLocation);
                } else {
                    Util.requestSupply(rc, 5000, myLocation);
                }
                return;
            }

            Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);

            if (dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, myLocation.add(dir), enemyRobots, false, groupOffset)) {
                rc.move(dir);
            }
        }
    }

    /*
     * if we should flee do so
     * otherwise move to the rally spot
     */
    private static void movingToRally() throws GameActionException {
        if (rc.isCoreReady()) {
            if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, false, groupOffset)) {
                Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
                if (d != Direction.NONE) {
                    rc.move(d);
                }
                return;
            }
            //if its
            Direction dir = Bug.startBuggin(rc, goal, 4);
            if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, myLocation.add(dir), enemyRobots, false, groupOffset)) {
                rc.move(dir);
                return;
            }
        }
    }

    /*
     * if we can attack someone do it
     * otherwise rally around the rally spot
     */
    private static void rallying() throws GameActionException {
        if (rc.isCoreReady()) {
            if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, true, groupOffset)) {
                Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
                if (d != Direction.NONE) {
                    rc.move(d);
                }
                return;
            }

            Direction dir = Bug.startBuggin(rc, goal, 4);
            if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, myLocation.add(dir), enemyRobots, false, groupOffset)) {
                rc.move(dir);
                return;
            }
        }
    }

    /*
     * if we should flee do so, broadcast the confronted enemies location if we have not temp attack loc
     * otherwise move towards the goal
     */
    private static void swarming() throws GameActionException {
        if (rc.isCoreReady()) {
            //if i can sense the goal and no one is there set to temp attack loc to none
            Util.debug(rc, "MOVE TO SWARM " + goal.toString());
            if (!GroupUtils.updateGoal(rc, goal, groupOffset)) {
                //could have been updated
                int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);

                goal = new MapLocation(x, y);
                goalType = RobotType.values()[goalTypeOrdinal];
            } else {
                state = RallyState.RALLYING_LEADER;
                makeMove();
                return;
            }

            if (GroupUtils.shouldFlee(rc, myLocation, enemyRobots, false, groupOffset)) {
                Direction d = GroupUtils.flee(rc, enemyRobots, myLocation);
                if (d != Direction.NONE) {
                    rc.move(d);
                }
                return;
            }
            //move to the goal but don't go in its attack radius
            Direction dir = Bug.startBuggin(rc, goal, goalType.attackRadiusSquared);
            Util.debug(rc, "after startbuggin!" + Clock.getBytecodeNum());
            if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir) && !GroupUtils.shouldFlee(rc, myLocation.add(dir), enemyRobots, false, groupOffset)) {
                rc.move(dir);
                return;
            }
        }
    }

    /*
     * if i can attack the goal do so
     * otherwise move in towards the goal
     *
     * returns false if the enemy has been destroyed
     */
    private static boolean collapsing() throws GameActionException {
        if (rc.isCoreReady()) {
            if (!GroupUtils.updateGoal(rc, goal, groupOffset)) {
                //could have been updated
                int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                int goalTypeOrdinal = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE);

                goal = new MapLocation(x,y);
                goalType = RobotType.values()[goalTypeOrdinal];

//                if (goalType == RobotType.LAUNCHER) {
//                    Direction d = myLocation.directionTo(goal);
//                    if (rc.canMove(d)) {
//                        rc.move(d);
//                        return true;
//                    } else {
//                        if (rc.canAttackLocation(goal)) {
//                            if (rc.isWeaponReady()) {
//                                rc.attackLocation(goal);
//                                return true;
//                            }
//                        } else {
//                            Util.tryMove(rc, d);
//                            return true;
//                        }
//                    }
//                }

                if (rc.canAttackLocation(goal)) {
                    if (rc.isWeaponReady()) {
                        rc.attackLocation(goal);
                    }
                } else {
                    Util.tryMove(rc, myLocation.directionTo(goal));
                }
                return true;
            } else {
                return false;
            }
        }

        return true;
    }
}