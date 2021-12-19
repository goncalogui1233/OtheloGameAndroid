package com.example.otello.utils

object ConstStrings {

    const val STATUS = "Status"

    /**Network Consts*/

    const val TYPE = "Type"
    const val START_GAME = "StartGame"
    const val STOP_GAME = "StopGame"
    const val PLAYER_INFO = "PlayerInfo"
    const val LEAVE_GAME = "LeaveGame"

    const val PLAYER_INFO_NOME = "Nome"
    const val PLAYER_INFO_PHOTO = "Foto"
    const val PLAYER_INFO_RESPONSE = "PInfoResponse"
    const val PLAYER_INFO_RESPONSE_VALID = "PInfoValid"
    const val PLAYER_INFO_RESPONSE_ACCEPTED = "Accepted"
    const val PLAYER_INFO_TOO_MANY_PLAYERS = "LobbyFull"

    const val PLAYER_ENTER_GAME = "EnterGame"

    /**NetworkActivity Consts*/
    const val INTENT_CONN_TYPE = "ConnectType"
    const val INTENT_IP_ADDR = "IpAddr"
    const val INTENT_PLAYER_LIST = "playersArray"
    const val BUNDLE = "Bundle"

    /**SharedPreferences Consts*/
    const val SHARED_PREFERENCES_INSTANCE = "ProfileInfo"
    const val SHARED_PREFERENCES_NAME = "PLAYER_NAME"
    const val SHARED_PREFERENCES_PHOTO = "PLAYER_PHOTO"


    const val INTENT_GAME_MODE = "GameMode"
    const val INTENT_GAME_LOCAL = "LocalGame"
    const val INTENT_GAME_ONLINE = "OnlineGame"

    /**Game Strings*/

    const val CLIENT_WANT_DATA = "WantData"
    const val GAME_INIT_INFOS = "InitialInfo"
    const val GAME_BOARD_DIMENSION = "BoardDimensions"
    const val BOARD_INIT_POSITIONS = "InitialPositions"
    const val BOARD_LINE = "Linha"
    const val BOARD_COLUMN = "Coluna"
    const val BOARD_POS_VALUE = "Value"
    const val PLAYER_NAME = "PlayerName"
    const val PLAYER_SCORE = "PlayerScore"
    const val PLAYERS_SCORES = "PlayersScores"

    const val PLAYER_ID = "PlayerID"
    const val PLAYER_PHOTO = "PlayerPhoto"
    const val CURRENT_PLAYER = "CurrentPlayer"

    const val GAME_PASS_TURN = "NextPlayer"
    const val GAME_BOMB_MOVE_ON = "BombMoveOn"
    const val GAME_PIECE_MOVE_ON = "PieceMoveOn"
    const val GAME_BOMB_MOVE_OFF = "BombMoveOff"
    const val GAME_PIECE_MOVE_OFF = "PieceMoveOff"
    const val GAME_BOMB_MOVE_ACTIVATED = "BombMoveActivated"
    const val GAME_BOMB_MOVE_DEACTIVATED = "BombMoveDectivated"
    const val GAME_BOMB_MOVE_ALREADY_ACTIVATED = "BombMoveIsActivated"
    const val GAME_PIECE_MOVE_ACTIVATED = "PieceMoveActivated"
    const val GAME_PIECE_MOVE_DEACTIVATED = "PieceMoveDectivated"
    const val GAME_PIECE_MOVE_ALREADY_ACTIVATED = "PieceMoveIsActivated"
    const val GAME_BOMB_MOVE_IS_ACTIVATED = "BombMoveIsActivated"
    const val GAME_PIECE_MOVE_IS_ACTIVATED = "ChangePieceMoveIsActivated"

    const val GAME_BOMB_MOVE_WAS_ACTIVATED = "BombMoveWasActivated"
    const val GAME_PIECE_MOVE_WAS_ACTIVATED = "PieceMoveWasActivated"


    const val GAME_BOMB_MOVE_ANSWER = "BombMoveAnswer"
    const val GAME_PIECE_MOVE_ANSWER = "PieceMoveAnswer"


    const val GAME_CHECK_PLACES = "CheckPlacesToPlay"
    const val GAME_PLACES_PLAY = "PlacesToPlay"
    const val GAME_PLACES = "Places"
    const val GAME_PLACED_PIECE = "PlacedNewPiece"
    const val GAME_PIECE_POSITION = "PlacedPiecePosition"
    const val GAME_PUT_NEW_PIECE = "PutNewPiece"
    const val GAME_NEW_POSITIONS = "NewPositions"
    const val GAME_VALID_PIECE = "ValidPlace"

}