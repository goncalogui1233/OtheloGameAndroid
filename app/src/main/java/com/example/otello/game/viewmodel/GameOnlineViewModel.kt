package com.example.otello.game.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.otello.game.model.*
import com.example.otello.game.repository.GameRepository
import com.example.otello.network.manager.NetworkManager
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.Socket
import kotlin.concurrent.thread

class GameOnlineViewModel : ViewModel() {

    val gameModel = GameRepository

    fun initBoard() {
        gameModel.endGame.postValue(EndGameStates.PLAYING)

        //Organize board when numJogadores = 2
        if (gameModel.numJogadores.value?.size == 2) {
            gameModel.board.value = Array(64) { IntArray(8) }
            gameModel.boardDimensions.value = 8
            gameModel.board.value!![3][3] = 1
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![4][3] = 2
            gameModel.board.value!![4][4] = 1

            gameModel.occupiedPlaces.postValue(4)
        } //Organize board when numJogadores = 3
        else if (gameModel.numJogadores.value?.size == 3) {
            gameModel.board.value = Array(100) { IntArray(10) }
            gameModel.boardDimensions.value = 10
            gameModel.board.value!![2][4] = 1
            gameModel.board.value!![2][5] = 2
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![3][5] = 1

            gameModel.board.value!![6][2] = 3
            gameModel.board.value!![6][3] = 1
            gameModel.board.value!![7][2] = 1
            gameModel.board.value!![7][3] = 3

            gameModel.board.value!![6][6] = 2
            gameModel.board.value!![6][7] = 3
            gameModel.board.value!![7][6] = 3
            gameModel.board.value!![7][7] = 2

            gameModel.occupiedPlaces.postValue(12)
        }

        alterarPontuacoes(gameModel.board.value!!)
    }


    /**
     * Função que insere uma nova peça no tabuleiro
     */
    fun updateValue(line: Int, column: Int) {
        val copyBoard = gameModel.board.value

        if (copyBoard != null && copyBoard[line][column] == 0) {
            //Check if it's possible to put piece in that position
            if (gameModel.checkIfPossible(line, column)) {
                var addedPieces = arrayListOf<AddedPosition>()
                //Coloca posição no board
                copyBoard[line][column] = gameModel.playerTurn.value?.id!!

                //Ver todas as peças e muda-las
                val newBoard: Array<IntArray>
                if (gameModel.bombMove.value!!) {
                    newBoard = gameModel.bombMove(copyBoard, line, column)
                    gameModel.playerTurn.value!!.bombPiece = false
                    gameModel.bombMove.postValue(false)

                    addedPieces = checkNeighboursForBomb(line, column)
                } else {
                    newBoard = changePieces(line, column, copyBoard)
                }

                //Recolhe todas as posições que já têm peça atribuida...
                for (i in 0 until gameModel.boardDimensions.value!!) {
                    for (j in 0 until gameModel.boardDimensions.value!!) {
                        if(copyBoard[i][j] != 0) {
                            addedPieces.add(AddedPosition(i,j,copyBoard[i][j]))
                        }
                    }
                }

                //Updates the number of pieces already filled
                gameModel.occupiedPlaces.postValue(addedPieces.size)

                //Alterar as pontuações dos jogadores
                alterarPontuacoes(newBoard)

                //Verificar se podemos continuar o jogo
                estadoJogo(newBoard)

                //Mudar de jogador
                val turnPlayer = checkNextPlayer()
                gameModel.playerTurn.postValue(turnPlayer)

                //Ver onde o próximo jogador pode jogar
                val validPositions = getPossiblePositions(turnPlayer, newBoard)
                gameModel.playPositions.value!!.clear()
                gameModel.playPositions.postValue(validPositions)

                //Atualizar o board
                gameModel.board.postValue(newBoard)

                //Envia infos para os outros jogadores...
                for (i in gameModel.numJogadores.value!!) {
                    if (i.gameSocket != null) {
                        val jsonData = sendTurnInfos(addedPieces, turnPlayer, validPositions)
                        NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
                    }
                }
            }
        }
    }

    /**
     * Applies special change piece to the board
     * Position 0 & 1 -> Pieces from the current player
     * Position 2 -> Piece from the other player
     */
    fun changePieceMove() {
        val copyBoard = gameModel.board.value!!
        val currPlayerPiece = copyBoard[gameModel.changePieceArray[0].linha][gameModel.changePieceArray[0].coluna]
        val otherPlayerPiece = copyBoard[gameModel.changePieceArray[2].linha][gameModel.changePieceArray[2].coluna]

        //As peças que o user deveriam dar não são iguais
        if(currPlayerPiece != copyBoard[gameModel.changePieceArray[1].linha][gameModel.changePieceArray[1].coluna]){
            return
        }
        else {
            //Altera as peças no board
            copyBoard[gameModel.changePieceArray[0].linha][gameModel.changePieceArray[0].coluna] = otherPlayerPiece
            copyBoard[gameModel.changePieceArray[1].linha][gameModel.changePieceArray[1].coluna] = otherPlayerPiece
            copyBoard[gameModel.changePieceArray[2].linha][gameModel.changePieceArray[2].coluna] = currPlayerPiece

            //Altera a propriedade para o jogador não poder usar este special
            gameModel.playerTurn.value?.pieceChange = false
            //Desliga o special no jogo
            gameModel.changePiecesMove.postValue(false)
            //Limpa o array
            gameModel.changePieceArray.clear()

            //Alterar as pontuações dos jogadores
            alterarPontuacoes(copyBoard)

            //Verificar se podemos continuar o jogo
            estadoJogo(copyBoard)

            //Mudar de jogador
            val turnPlayer = checkNextPlayer()
            gameModel.playerTurn.postValue(turnPlayer)

            //Recolhe todas as posições que já têm peça atribuida...
            val addedPieces = arrayListOf<AddedPosition>()
            for (i in 0 until gameModel.boardDimensions.value!!) {
                for (j in 0 until gameModel.boardDimensions.value!!) {
                    if(copyBoard[i][j] != 0) {
                        addedPieces.add(AddedPosition(i,j,copyBoard[i][j]))
                    }
                }
            }

            //Updates the number of pieces already filled
            gameModel.occupiedPlaces.postValue(addedPieces.size)

            //Ver onde o próximo jogador pode jogar
            val validPositions = getPossiblePositions(turnPlayer, copyBoard)
            gameModel.playPositions.postValue(validPositions)

            //Altera o board
            gameModel.board.postValue(copyBoard)

            //Sends info to the player...
            for (i in gameModel.numJogadores.value!!) {
                if (i.gameSocket != null) {
                    val jsonData = sendTurnInfos(addedPieces, turnPlayer, validPositions)
                    NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
                }
            }
        }
    }

    /**
     * Function that checks if the board is already complete
     */
    fun estadoJogo(board: Array<IntArray>) {
        for (i in 0 until gameModel.boardDimensions.value!!) {
            for (j in 0 until gameModel.boardDimensions.value!!) {
                if (board[i][j] == 0) {
                    return
                }
            }
        }
        gameModel.endGame.postValue(EndGameStates.FINISHED)
    }

    /**
     * Function that check if a player still can play
     */
    fun checkPlay() {
        gameModel.checkPlayerMoves()
    }

    /**
     * Após um jogador inserir uma peça no tabuleiro, esta função é chamada para
     * alterar as peças já existentes no tabuleiro
     */
    fun changePieces(line: Int, column: Int, copyBoard: Array<IntArray>): Array<IntArray> {

        //Check Left
        gameModel.flipLine(copyBoard, line, column, true)

        //Check Top Left Diagonal
        gameModel.flipTopDiagonal(copyBoard, line, column, true)

        //Check Top
        gameModel.flipColumn(copyBoard, line, column, true)

        //check top right diagonal
        gameModel.flipTopDiagonal(copyBoard, line, column, false)

        //Check Right
        gameModel.flipLine(copyBoard, line, column, false)

        //Check Bottom Right Diagonal
        gameModel.flipDiagonalBottom(copyBoard, line, column, false)

        //Check Bottom
        gameModel.flipColumn(copyBoard, line, column, false)

        //check bottom left diagonal
        gameModel.flipDiagonalBottom(copyBoard, line, column, true)

        return copyBoard
    }

    /**
     * Esta função percorre o board para contar o numero de peças de cada jogador
     */
    fun alterarPontuacoes(board: Array<IntArray>) {
        val pont = arrayListOf(0, 0, 0)

        for (i in 0 until gameModel.boardDimensions.value!!) {
            for (j in 0 until gameModel.boardDimensions.value!!) {
                when (board[i][j]) {
                    1 -> pont[0]++
                    2 -> pont[1]++
                    3 -> pont[2]++
                }
            }
        }

        for (i in 0 until gameModel.numJogadores.value!!.size) {
            gameModel.numJogadores.value!![i].score = pont[i]
        }
    }

    /**
     * Esta função altera o jogador atual e vê quais os locais onde ele pode jogar.
     */
    fun checkNextPlayer(player: Int = -1) : Jogador {
        if (player == -1) {
            if (gameModel.playerTurn.value?.id!! == gameModel.numJogadores.value?.size!!) {
                return gameModel.numJogadores.value?.get(0)!!
            } else {
                return gameModel.numJogadores.value?.get(gameModel.numJogadores.value?.indexOf(gameModel.playerTurn.value!!)?.plus(1)!!)!!
            }
        } else {
            return gameModel.numJogadores.value?.get(player - 1)!!
        }
    }

    /**
     * Baseado no jogador atual, esta função procura um local para o jogador jogar
     */
    fun getPossiblePositions(player: Jogador, boardCheck: Array<IntArray>? = null) : ArrayList<Posicoes> {
        var board : Array<IntArray>?

        if(boardCheck == null) {
            board = gameModel.board.value
        }
        else {
            board = boardCheck
        }

        if (board != null) {
            val k = arrayListOf<Posicoes>()

            for (i in 0 until gameModel.boardDimensions.value!!) {
                for (j in 0 until gameModel.boardDimensions.value!!) {
                    if (board[i][j] != 0 && board[i][j] != player.id) {

                        var pos: Posicoes? = null
                        //Check Left
                        pos = gameModel.searchBoardLine(i, j, true, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Left
                        pos = gameModel.searchBoardDiagonalTop(i, j, true, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Top
                        pos = gameModel.searchBoardColumn(i, j, true, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Right
                        pos = gameModel.searchBoardDiagonalTop(i, j, false, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Right
                        pos = gameModel.searchBoardLine(i, j, false, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Right
                        pos = gameModel.searchBoardDiagonalBottom(i, j, false, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Bottom
                        pos = gameModel.searchBoardColumn(i, j, false, player)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Left
                        pos = gameModel.searchBoardDiagonalBottom(i, j, true, player)
                        if (pos != null)
                            k.add(pos)
                    }
                }
            }
            return k
        }
        return arrayListOf()
    }

    fun receiveInfoFromClients(socket: Socket) {
        thread {
            while (true) {
                var str = ""
                try {
                    str = BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
                } catch (e: Exception) {
                    return@thread
                }

                try {
                    val json = JSONObject(str)
                    when (json.optString(ConstStrings.TYPE)) {
                        ConstStrings.CLIENT_WANT_DATA -> {
                            //Send board size and positions filled to clients
                            val jsonData = JSONObject()
                            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_INIT_INFOS)
                            jsonData.put(ConstStrings.GAME_BOARD_DIMENSION, gameModel.boardDimensions.value!!)

                            val posJson = JSONArray()
                            for (i in 0 until gameModel.boardDimensions.value!!) {
                                for (j in 0 until gameModel.boardDimensions.value!!) {
                                    if (gameModel.board.value!![i][j] != 0) {
                                        val jsonObject = JSONObject()
                                        jsonObject.put(ConstStrings.BOARD_LINE, i)
                                        jsonObject.put(ConstStrings.BOARD_COLUMN, j)
                                        jsonObject.put(ConstStrings.BOARD_POS_VALUE, gameModel.board.value!![i][j])
                                        posJson.put(jsonObject)
                                    }
                                }
                            }
                            jsonData.put(ConstStrings.BOARD_INIT_POSITIONS, posJson)

                            //Send Scores
                            val pontArray = JSONArray()
                            for (p in gameModel.numJogadores.value!!) {
                                val jsonPlayer = JSONObject()
                                jsonPlayer.put(ConstStrings.PLAYER_NAME, p.name)
                                jsonPlayer.put(ConstStrings.PLAYER_SCORE, p.score)
                                jsonPlayer.put(ConstStrings.PLAYER_ID, p.id)
                                pontArray.put(jsonPlayer)
                            }
                            jsonData.put(ConstStrings.PLAYERS_SCORES, pontArray)

                            //Send current player to play

                            val currentPlayer = JSONObject()
                            currentPlayer.put(ConstStrings.PLAYER_ID, gameModel.playerTurn.value!!.id)
                            currentPlayer.put(ConstStrings.PLAYER_NAME, gameModel.playerTurn.value!!.name)
                           /* if (gameModel.playerTurn.value!!.photo != null) {
                                val jsonObject = JSONObject().put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(gameModel.playerTurn.value!!.photo!!))
                                currentPlayer.put(ConstStrings.PLAYER_PHOTO, jsonObject)
                            }*/
                            jsonData.put(ConstStrings.CURRENT_PLAYER, currentPlayer)

                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }

                        ConstStrings.GAME_PASS_TURN -> {
                            val nextPlayer = checkNextPlayer()
                            gameModel.playerTurn.postValue(nextPlayer)
                            gameModel.playPositions.postValue(getPossiblePositions(nextPlayer))
                            val jsonData = JSONObject()
                            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
                            jsonData.put(ConstStrings.CURRENT_PLAYER, JSONObject().put(ConstStrings.PLAYER_ID, nextPlayer.id)
                                    .put(ConstStrings.PLAYER_NAME, nextPlayer.name))

                            for(i in gameModel.numJogadores.value!!) {
                                if(i.gameSocket != null) {
                                    NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
                                }
                            }
                        }

                        ConstStrings.GAME_PLAYER_SEE_MOVES -> {
                            if(gameModel.playerTurn.value?.seeMoves!!) {
                                gameModel.playerTurn.value?.seeMoves = false
                            }
                            else {
                                gameModel.playerTurn.value?.seeMoves = true
                                //Sends the moves to player
                                val jsonData = JSONObject()
                                jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_POSSIBLE_POSITIONS)
                                val movesArray = JSONArray()
                                for(i in gameModel.playPositions.value!!) {
                                    movesArray.put(JSONObject().put(ConstStrings.BOARD_LINE, i.linha)
                                        .put(ConstStrings.BOARD_COLUMN, i.coluna))
                                }
                                jsonData.put(ConstStrings.GAME_POSSIBLE_POSITIONS, movesArray)
                                NetworkManager.sendInfo(socket, jsonData.toString())
                            }
                        }

                        ConstStrings.GAME_BOMB_MOVE_ON -> {
                            val jsonData = JSONObject()
                            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_BOMB_MOVE_ANSWER)

                            if (!gameModel.playerTurn.value!!.bombPiece) {
                                jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_BOMB_MOVE_WAS_ACTIVATED)
                            } else {
                                if (!gameModel.changePiecesMove.value!!) {
                                    if (!gameModel.bombMove.value!!) {
                                        gameModel.bombMove.postValue(true)
                                        jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_BOMB_MOVE_ACTIVATED)
                                    } else {
                                        gameModel.bombMove.postValue(false)
                                        jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_BOMB_MOVE_DEACTIVATED)
                                    }
                                } else {
                                    jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_PIECE_MOVE_IS_ACTIVATED)
                                }
                            }
                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }

                        ConstStrings.GAME_PIECE_MOVE_ON -> {
                            val jsonData = JSONObject()
                            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PIECE_MOVE_ANSWER)

                            if (!gameModel.playerTurn.value!!.pieceChange) {
                                //Jogador já ativou uma vez este special...
                                jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_PIECE_MOVE_WAS_ACTIVATED)
                            } else {
                                if (!gameModel.bombMove.value!!) {
                                    if (!gameModel.changePiecesMove.value!!) {
                                        //Ativa o special
                                        gameModel.changePiecesMove.postValue(true)
                                        jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_PIECE_MOVE_ACTIVATED)
                                    } else {
                                        //Desativa o special
                                        gameModel.changePiecesMove.postValue(false)
                                        jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_PIECE_MOVE_DEACTIVATED)
                                    }
                                } else {
                                    //O outro special já está ativo
                                    jsonData.put(ConstStrings.STATUS, ConstStrings.GAME_BOMB_MOVE_IS_ACTIVATED)
                                }
                            }
                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }

                        ConstStrings.GAME_CHECK_PLACES -> {
                            val jsonData = JSONObject()
                            val jsonArray = JSONArray()
                            for (i in gameModel.playPositions.value!!) {
                                val jsonObj = JSONObject()
                                jsonObj.put(ConstStrings.BOARD_LINE, i.linha)
                                jsonObj.put(ConstStrings.BOARD_COLUMN, i.coluna)
                                jsonArray.put(jsonObj)
                            }
                            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PLACES_PLAY)
                            jsonData.put(ConstStrings.GAME_PLACES, jsonArray)
                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }

                        ConstStrings.GAME_PLACED_PIECE -> {
                            val linha = json.optJSONObject(ConstStrings.GAME_PIECE_POSITION)?.optInt(ConstStrings.BOARD_LINE)!!
                            val coluna = json.optJSONObject(ConstStrings.GAME_PIECE_POSITION)?.optInt(ConstStrings.BOARD_COLUMN)!!
                            val jsonData = JSONObject()

                            if(gameModel.changePiecesMove.value!!) {
                                gameModel.changePieceArray.add(Posicoes(linha, coluna))
                                if(gameModel.changePieceArray.size == 3) {
                                    changePieceMove()
                                }
                            }
                            else {
                                if (gameModel.checkIfPossible(linha, coluna)) {
                                    updateValue(linha, coluna)
                                } else {
                                    jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PUT_NEW_PIECE)
                                    jsonData.put(ConstStrings.GAME_VALID_PIECE, false)
                                    NetworkManager.sendInfo(socket, jsonData.toString())
                                }
                            }
                        }

                        ConstStrings.GAME_UPDATE_INFOS -> {
                            val addedPieces = arrayListOf<AddedPosition>()
                            //Recolhe todas as posições que já têm peça atribuida...
                            for (i in 0 until gameModel.boardDimensions.value!!) {
                                for (j in 0 until gameModel.boardDimensions.value!!) {
                                    if(gameModel.board.value!![i][j] != 0) {
                                        addedPieces.add(AddedPosition(i,j,gameModel.board.value!![i][j]))
                                    }
                                }
                            }

                            val jsonObject = sendTurnInfos(addedPieces, gameModel.playerTurn.value!!, gameModel.playPositions.value!!)
                            jsonObject.put(ConstStrings.TYPE, ConstStrings.GAME_PUT_NEW_PIECE)
                            NetworkManager.sendInfo(socket, jsonObject.toString())
                        }

                        ConstStrings.GAME_END_ABRUPTLY -> {
                            gameModel.endGame.postValue(EndGameStates.ABRUPTLY)
                            return@thread
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Receive Info Client", e.toString())
                }
            }
        }
    }

    fun initComunication() {
        for (p in gameModel.numJogadores.value!!) {
            if (p.gameSocket != null) {
                receiveInfoFromClients(p.gameSocket!!)
            }
        }
    }

    fun passTurn() {
        if(gameModel.playerTurn.value?.id == NetworkManager.playerId) {
            gameModel.playerTurn.value = checkNextPlayer()
            gameModel.playPositions.value = getPossiblePositions(gameModel.playerTurn.value!!)

            val jsonData = JSONObject()
            jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
            val nextPlayer = JSONObject().put(ConstStrings.PLAYER_ID, gameModel.playerTurn.value!!.id)
                    .put(ConstStrings.PLAYER_NAME, gameModel.playerTurn.value!!.name)
            if (gameModel.playerTurn.value!!.photo != null) {
                nextPlayer.put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(gameModel.playerTurn.value!!.photo!!))
            }

            jsonData.put(ConstStrings.CURRENT_PLAYER, nextPlayer)

            for(i in gameModel.numJogadores.value!!) {
                if(i.gameSocket != null) {
                    NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
                }
            }
        }
    }

    /**
     * Warns every player that the server ended the game abruptly
     */
    fun serverLeaveGame() {
        var winner = gameModel.numJogadores.value!![0]
        for (i in 1 until gameModel.numJogadores.value?.size!!) {
            if (gameModel.numJogadores.value!![i].score > winner.score) {
                winner = gameModel.numJogadores.value!![i]
            }
        }

        gameModel.playerWinner.value = winner

        val json = JSONObject().put(ConstStrings.TYPE, ConstStrings.GAME_END_ABRUPTLY)
                .put(ConstStrings.PLAYER_NAME, winner.name)
                .put(ConstStrings.PLAYER_SCORE, winner.score)

        for(i in gameModel.numJogadores.value!!)
            if(i.gameSocket != null) {
                NetworkManager.sendInfo(i.gameSocket!!, json.toString())
            }


    }

    fun calculateWinner() {
        var winner = gameModel.numJogadores.value!![0]
        for (i in 1 until gameModel.numJogadores.value?.size!!) {
            if (gameModel.numJogadores.value!![i].score > winner.score) {
                winner = gameModel.numJogadores.value!![i]
            }
        }

        gameModel.playerWinner.postValue(winner)
    }


    private fun checkNeighboursForBomb(line: Int, column: Int): ArrayList<AddedPosition> {

        val add = arrayListOf<AddedPosition>()

        if(line - 1 >= 0 && column -1 >= 0) {
            add.add(AddedPosition(line-1, column-1, 0))
        }

        if(line - 1 >= 0) {
            add.add(AddedPosition(line - 1, column, 0))
        }

        if(line - 1 >= 0 && column + 1 < gameModel.boardDimensions.value!!) {
            add.add(AddedPosition(line-1, column+1, 0))
        }

        if(column + 1 < gameModel.boardDimensions.value!!) {
            add.add(AddedPosition(line, column-1, 0))
        }

        if(line + 1 < gameModel.boardDimensions.value!! && column + 1 < gameModel.boardDimensions.value!!) {
            add.add(AddedPosition(line+1, column+1, 0))
        }

        if(line + 1 < gameModel.boardDimensions.value!!) {
            add.add(AddedPosition(line+1, column, 0))
        }

        if(line + 1 < gameModel.boardDimensions.value!! && column - 1 >= 0) {
            add.add(AddedPosition(line+1, column-1, 0))
        }

        if(column - 1 >= 0) {
            add.add(AddedPosition(line, column-1, 0))
        }

        return add
    }

    private fun sendTurnInfos(addedPieces : ArrayList<AddedPosition>, turnPlayer : Jogador, validPosicoes: ArrayList<Posicoes>) : JSONObject {
        val jsonData = JSONObject()
        jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PUT_NEW_PIECE)
        jsonData.put(ConstStrings.GAME_VALID_PIECE, true)

        //Peças preenchidas no board
        val jsonArray = JSONArray()
        for (added in addedPieces) {
            jsonArray.put(JSONObject().put(ConstStrings.BOARD_LINE, added.linha)
                    .put(ConstStrings.BOARD_COLUMN, added.coluna)
                    .put(ConstStrings.BOARD_POS_VALUE, added.value))
        }
        jsonData.put(ConstStrings.GAME_NEW_POSITIONS, jsonArray)

        jsonData.put(ConstStrings.GAME_NUMBER_MOVES, validPosicoes.size)

        //Se é suposto o jogador ver os movimentos
        if(turnPlayer.seeMoves) {
            val movesArray = JSONArray()
            for(i in validPosicoes) {
                movesArray.put(JSONObject().put(ConstStrings.BOARD_LINE, i.linha)
                    .put(ConstStrings.BOARD_COLUMN, i.coluna))
            }
            jsonData.put(ConstStrings.GAME_POSSIBLE_POSITIONS, movesArray)
        }

        //Pontuações
        val pontArray = JSONArray()
        for (p in gameModel.numJogadores.value!!) {
            val jsonPlayer = JSONObject()
            jsonPlayer.put(ConstStrings.PLAYER_NAME, p.name)
            jsonPlayer.put(ConstStrings.PLAYER_SCORE, p.score)
            jsonPlayer.put(ConstStrings.PLAYER_ID, p.id)
            pontArray.put(jsonPlayer)
        }
        jsonData.put(ConstStrings.PLAYERS_SCORES, pontArray)

        //Proximo jogador
        val nextPlayer = JSONObject().put(ConstStrings.PLAYER_ID, turnPlayer.id)
                .put(ConstStrings.PLAYER_NAME, turnPlayer.name)
       /* if (turnPlayer.photo != null) {
            if (gameModel.playerTurn.value!!.photo != null) {
                val jsonObject = JSONObject().put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(gameModel.playerTurn.value!!.photo!!))
                nextPlayer.put(ConstStrings.PLAYER_PHOTO, jsonObject)
            }
        }*/

        jsonData.put(ConstStrings.GAME_PASS_TURN, nextPlayer)

        return jsonData
    }

}