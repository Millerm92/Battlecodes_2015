package team302;

public class MyConstants {
    public static int ROBOT_COUNT_OFFSET = 0;               // 0 - 20
    public static int SPAWN_TYPE_OFFSET = 21;               // 21 - 42
    public static int ALONE_TOWER_INDEX = 66;               // 66 stores the index of the tower we want to send launcher to protect
    public static int TOWER_UNDER_DISTRESS = 67;            // 67 occupies 67-72 (6, 1 for each tower)
    public static int TOWER_UNDER_DISTRESS_LOCATION = 73;   // 73 - 84 (12, 1 for each towers x and y position)
    public static int TARGET_TOWER_X = 85;                  // 85
    public static int TARGET_TOWER_Y= 86;                   // 86
    public static int TOWER_ORE = 87;                       // 87 - 92 (6, 1 for each tower)
    public static int AVG_ORE = 285;                        // 285 is an average of the ore surrounding the towers
    public static int MOST_ORE = 286;                       // 286 amount of ore at MOST_ORE_LOCATION
    public static int MOST_ORE_LOCATION_X = 287;            // 287 units should check this and if they see more ore than this, update this value.
    public static int MOST_ORE_LOCATION_Y = 288;            // 288
    public static int MY_TOWER_HEALTH = 289;                // 289 - 300 (12, 1 for roundnum, 1 for hp)
    public static int ENEMY_TOWER_HEALTH = 301;             // 301 - 306 (6, 1 for each enemy tower)
    public static int ENEMY_HQ_HP = 307;                    // 307
    public static int MAIN_GOAL = 600;						// 308 - 309 safest tower to attack or hq
    public static int COMMANDER_SUPPLY_REQUEST_OFFSET = 309;// 309 - 312
    public static int MAX_TANK_FACTORIES_OFFSET = 314;
    public static int MAX_BARRACKS_OFFSET = 315;
    public static int MAX_SUPPLY_DEPOTS_OFFSET = 316;
     
    
    public static int ATTACK_GROUP_OFFSET = 320;
    public static int SCOUT_GROUP_OFFSET = 340;
    public static int SCOUT_LEADER_OFFSET = 360;
    public static int GROUP_LEADER_STAY_ALIVE = 380;
    public static int NUM_KILL_TARGETS_OFFSET = 381;
    public static int KILL_TARGETS_OFFSET = 382;            // 382 - 581
    public static int SUPPLY_DRONES = 582;                  // 582 - 585, each supply drone will be updating one of these with a round num
    
    public static int NUM_CONTAINING_ENEMIES_OFFSET = 586;	// 586
    public static int TEAM_IN_ROUTE = 587;					// 587
    public static int CONTAINING_ENEMY_LOCATION_OFFSET = 588;// 588 - 687

    public static int GROUP_OFFSET = 20000;  
    public static int MINER_INFO = 40000;                      // 95 - 194 (100, 4 for each miner (round Number, status, x location, y location))
    public static int REQUEST_HQ_SUPPLIES_OFFSET = 50000;   // 193 - 284
    public static int NUM_POINTS_OF_INTEREST_OFFSET = 60000; // will occupy channel 60000;
    public static int POINTS_OF_INTEREST_OFFSET = 60001; // will occupy channels 60,001 - ?;
    

//	{HQ, TOWER, SUPPLYDEPOT, TECHInst, BARR, HELI, TFIELD, TANKFACT, MINORFACT, HANDWASH, AEROSPACE, 
//	 BEAVER, COMPUTER, SOLDIER, BASHER, MINER, DRONE, TANK, COMMANDER, LAUNCHER, MISSILE};
    
    public static int[] assaultPriorities = {1, 1, 6, 6, 4, 4, 5, 4, 4, 6, 5,
    										 5, 1, 4, 4, 5, 5, 3, 2, 1, 0};
    public static int[] harassPriorities = {2, 2, 6, 6, 4, 4, 5, 4, 4, 6, 4,
    										3, 1, 4, 4, 3, 3, 5, 5, 5, 0};
    public static int[] cowardPriorities = {6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    										3, 1, 4, 4, 3, 5, 5, 5, 5, 1};

}