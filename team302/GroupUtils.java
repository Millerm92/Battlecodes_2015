package team302;
import battlecode.common.*;

public class GroupUtils {
    public static Direction[] allDirections = Direction.values();

    public static void updateSwarmCount(RobotController rc, MapLocation goal, RobotType goalType, int groupOffset) throws GameActionException {
        int thresh;
        if (goalType == RobotType.HQ) {
            thresh = 16;
        } else {
            thresh = 8;
        }

        if (rc.getLocation().distanceSquaredTo(goal) <= goalType.attackRadiusSquared + thresh) {
            Util.debug(rc, "casting!" + Clock.getBytecodeNum());
            rc.broadcast(groupOffset + GroupConstants.SWARM_POWER, (int) (rc.readBroadcast(groupOffset + GroupConstants.SWARM_POWER) + (RobotPlayer.type.attackPower / RobotPlayer.type.attackDelay)));
            rc.broadcast(groupOffset + GroupConstants.SWARM_HEALTH, (int) (rc.readBroadcast(groupOffset + GroupConstants.SWARM_HEALTH) + rc.getHealth()));

            int x = rc.readBroadcast(MyConstants.MAIN_GOAL);
            int y = rc.readBroadcast(MyConstants.MAIN_GOAL + 1);
            MapLocation pointOfInterest = new MapLocation(x,y);

            if (goal.equals(pointOfInterest)) {
                rc.broadcast(MyConstants.MAIN_GOAL + 2, (int) (rc.readBroadcast(MyConstants.MAIN_GOAL + 2) + (RobotPlayer.type.attackPower / RobotPlayer.type.attackDelay)));
                rc.broadcast(MyConstants.MAIN_GOAL + 3, (int) (rc.readBroadcast(MyConstants.MAIN_GOAL + 3) + rc.getHealth()));
            }
        }

    }


	/*
     * returns true if someone can hit me at my current location
     *
     * broadcast - should i broadcast the enemies location as a temporary goal
     */
    public static boolean shouldFlee(RobotController rc, MapLocation loc, RobotInfo[] enemyRobots, boolean broadcast, int groupOffset) throws GameActionException {
        for (RobotInfo robot: enemyRobots) {
                double distanceAfterMovingTowards;
                //if its a tower or hq we just have if it can hit the location
                //else its a moving bot (or other structure that cannot attack)
                //so we check if that bot moves towards us then can he hit us
                if (robot.type == RobotType.TOWER || robot.type == RobotType.HQ) {
                    distanceAfterMovingTowards = loc.distanceSquaredTo(robot.location);
                } else {
                    distanceAfterMovingTowards = loc.distanceSquaredTo(robot.location.add(robot.location.directionTo(loc)));
                }

                int attackRad;
                if (robot.type == RobotType.LAUNCHER) {
                    attackRad = 25;
                } else {
                    attackRad = robot.type.attackRadiusSquared;
                }

                // if he moved towards me could he hit me???
                if (distanceAfterMovingTowards <= attackRad) {
                    int extraSplash = (RobotPlayer.enemyTowers.length > 4) ? 50 : 35;
                    if (broadcast && robot.type != RobotType.LAUNCHER && robot.location.distanceSquaredTo(RobotPlayer.enemyHq) > extraSplash) {
                        int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                        int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                        if (x == 0 && y == 0) {
                            rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, robot.location.x);
                            rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, robot.location.y);
                            rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, robot.type.ordinal());
                        }
                    }
                    return true;
                }

        }

        //towers too
        for (MapLocation tloc: RobotPlayer.enemyTowers) {
            if (loc.distanceSquaredTo(tloc) <= RobotType.TOWER.attackRadiusSquared) {
                if (broadcast) {
                    int x = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
                    int y = rc.readBroadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
                    if (x == 0 && y == 0) {
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, tloc.x);
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, tloc.y);
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, RobotType.TOWER.ordinal());
                    }
                }
                return true;
            }
        }

        if (loc.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
            return true;
        }

        return false;
    }

    /*
     * Finds a location where no enemies can attack
     *
     *
     * if no such location exists return false
     */
    public static Direction flee(RobotController rc, RobotInfo[] enemyRobots, MapLocation myLocation) throws GameActionException {
        MapLocation desiredLoc;
    	double distanceAfterMovingTowards;

        dirLoop:
        for (Direction d: allDirections) {
            if (d.equals(Direction.OMNI) || d.equals(Direction.NONE)) {
                continue;
            }
            desiredLoc = myLocation.add(d);

            //if i can move in the given direction
            if (rc.canMove(d)) {

                //iterate through each enemy bot
                for (RobotInfo robot : enemyRobots) {
                        //if its a tower or hq we just have if it can hit the location
                        //else its a moving bot (or other structure that cannot attack)
                        //so we check if that bot moves towards us then can he hit us
                        if (robot.type == RobotType.TOWER || robot.type == RobotType.HQ) {
                            distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location);
                        } else {
                            distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc)));
                        }

                        int attackRad;
                        if (robot.type == RobotType.LAUNCHER) {
                            attackRad = 25;
                        } else {
                            attackRad = robot.type.attackRadiusSquared;
                        }

                        //could he hit me if he moved in
                        if (distanceAfterMovingTowards <= attackRad) {
                            // could he out chase me if i fled to where he cant hit me now?
                            if (robot.coreDelay < 1 || desiredLoc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
                                continue dirLoop;
                            }
                        }

                }
                //can tower hit me
                for (MapLocation tloc: RobotPlayer.enemyTowers) {
                    if (desiredLoc.distanceSquaredTo(tloc) <= RobotType.TOWER.attackRadiusSquared) {
                        continue dirLoop;
                    }
                }
                //can hq hit me
                if (desiredLoc.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
                    continue dirLoop;
                }

                return d;
            }
        }

        return Direction.NONE;
    }

    // returns true if we got the fucker
    // updates his location if he moved
    public static boolean updateGoal(RobotController rc, MapLocation goal, int groupOffset) throws GameActionException {
        if (rc.canSenseLocation(goal)) {

            RobotInfo botAtGoal = rc.senseRobotAtLocation(goal);
            if (botAtGoal != null && botAtGoal.team == RobotPlayer.enemyTeam) {
                return false;
            }

        	//check if he moved
        	MapLocation added;
            for (Direction d: allDirections) {
                if (d.equals(Direction.OMNI) || d.equals(Direction.NONE)) {
                    continue;
                }
            	added = goal.add(d);
                if (rc.canSenseLocation(added)) {
                    botAtGoal = rc.senseRobotAtLocation(added);
                    if (botAtGoal != null && botAtGoal.team == RobotPlayer.enemyTeam) {
                        goal = botAtGoal.location;
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, goal.x);
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, goal.y);
                        rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_TYPE, botAtGoal.type.ordinal());
                        return false;
                    }
                }
            }

            rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION, 0);
            rc.broadcast(groupOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1, 0);
            return true;
        }

        return false;
    }

}