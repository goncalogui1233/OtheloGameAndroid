package com.example.otello.game.viewmodel

import androidx.lifecycle.ViewModel
import com.example.otello.game.model.AddedPosition
import com.example.otello.game.model.GameModel
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
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

    val gameModel = GameModel

    fun initBoard(boardSize: Int, boardDimen: Int, numPlayers: Int) {
        //Inicia o Board com todas as posições vazias e guarda o num de colunas e linhas
        gameModel.board.value = Array(boardSize) { IntArray(boardSize) }
        gameModel.boardDimensions.value = boardDimen

        //Organize board when numJogadores = 2
        if (gameModel.numJogadores.value?.size == 2) {
            gameModel.board.value!![3][3] = 1
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![4][3] = 2
            gameModel.board.value!![4][4] = 1
        } //Organize board when numJogadores = 3
        else if (gameModel.numJogadores.value?.size == 3) {
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
            if (checkIfPossible(line, column)) {
                //Coloca posição no board
                copyBoard[line][column] = gameModel.playerTurn.value?.id!!

                //Ver todas as peças e muda-las
                val newBoard: Array<IntArray>
                if (gameModel.bombMove.value!!) {
                    newBoard = bombMove(copyBoard, line, column)
                    gameModel.playerTurn.value!!.bombPiece = false
                    gameModel.bombMove.value = false
                } else {
                    newBoard = changePieces(line, column, copyBoard)
                }

                //Compara os boards para ver as peças que mudaram
                gameModel.addedPieces = arrayListOf()
                for (i in 0 until gameModel.boardDimensions.value!!) {
                    for (j in 0 until gameModel.boardDimensions.value!!) {
                        if(copyBoard[i][j] != 0) {
                            gameModel.addedPieces.add(AddedPosition(i,j,copyBoard[i][j]))
                        }
                    }
                }

                //Alterar as pontuações dos jogadores
                alterarPontuacoes(newBoard)

                //Atualizar o board
                gameModel.board.postValue(newBoard)

                //Verificar se podemos continuar o jogo
                estadoJogo(newBoard)

                //Mudar de jogador
                gameModel.playerTurn.postValue(checkNextPlayer())
            }
        }
    }

    fun bombMove(board: Array<IntArray>, line: Int, column: Int): Array<IntArray> {
        val copyBoard = board

        //Left
        if (column - 1 >= 0) {
            copyBoard[line][column - 1] = 0
        }

        //Diagonal Top Left
        if (line - 1 >= 0 && column - 1 >= 0) {
            copyBoard[line - 1][column - 1] = 0
        }
        //Top
        if (line - 1 >= 0) {
            copyBoard[line - 1][column] = 0
        }

        //Diagonal Top Right
        if (line - 1 >= 0 && column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line - 1][column + 1] = 0
        }

        //Right
        if (column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line][column + 1] = 0
        }

        //Diagonal Bottom Right
        if (line + 1 < GameModel.boardDimensions.value!! && column + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line + 1][column + 1] = 0
        }

        //Bottom
        if (line + 1 < GameModel.boardDimensions.value!!) {
            copyBoard[line + 1][column] = 0
        }

        //Diagonal Bottom Left
        if (line + 1 < GameModel.boardDimensions.value!! && column - 1 >= 0) {
            copyBoard[line + 1][column - 1] = 0
        }

        //Desliga o special no modelo do jogo
        GameModel.bombMove.value = false

        return copyBoard
    }

    /**
     * Applies special change piece to the board
     * Position 0 & 1 -> Pieces from the current player
     * Position 2 -> Piece from the other player
     */
    fun changePieceMove() {
        val copyBoard = GameModel.board.value!!
        val currPlayerPiece = copyBoard[GameModel.changePieceArray[0].linha][GameModel.changePieceArray[0].coluna]
        val otherPlayerPiece = copyBoard[GameModel.changePieceArray[2].linha][GameModel.changePieceArray[2].coluna]

        //Altera as peças no board
        copyBoard[GameModel.changePieceArray[0].linha][GameModel.changePieceArray[0].coluna] = otherPlayerPiece
        copyBoard[GameModel.changePieceArray[1].linha][GameModel.changePieceArray[1].coluna] = otherPlayerPiece
        copyBoard[GameModel.changePieceArray[2].linha][GameModel.changePieceArray[2].coluna] = currPlayerPiece

        //Altera a propriedade para o jogador não poder usar este special
        GameModel.playerTurn.value?.pieceChange = false
        //Desliga o special no jogo
        GameModel.changePiecesMove.value = false

        //Alterar as pontuações dos jogadores
        alterarPontuacoes(copyBoard)

        //Verificar se podemos continuar o jogo
        estadoJogo(copyBoard)

        //Altera o board
        gameModel.board.value = copyBoard

        //Mudar de jogador
        gameModel.playerTurn.postValue(checkNextPlayer())

    }

    fun estadoJogo(board: Array<IntArray>) {
        for (i in 0 until gameModel.boardDimensions.value!!) {
            for (j in 0 until gameModel.boardDimensions.value!!) {
                if (board[i][j] == 0) {
                    return
                }
            }
        }
        gameModel.endGame.postValue(true)
    }

    /**
     * Após um jogador inserir uma peça no tabuleiro, esta função é chamada para
     * alterar as peças já existentes no tabuleiro
     */
    fun changePieces(line: Int, column: Int, copyBoard: Array<IntArray>): Array<IntArray> {

        //Check Left
        flipLine(copyBoard, line, column, true)

        //Check Top Left Diagonal
        flipTopDiagonal(copyBoard, line, column, true)

        //Check Top
        flipColumn(copyBoard, line, column, true)

        //check top right diagonal
        flipTopDiagonal(copyBoard, line, column, false)

        //Check Right
        flipLine(copyBoard, line, column, false)

        //Check Bottom Right Diagonal
        flipDiagonalBottom(copyBoard, line, column, false)

        //Check Bottom
        flipColumn(copyBoard, line, column, false)

        //check bottom left diagonal
        flipDiagonalBottom(copyBoard, line, column, true)

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
    fun getPossiblePositions() {
        val board = gameModel.board.value

        if (board != null) {
            val k = arrayListOf<Posicoes>()

            for (i in 0 until gameModel.boardDimensions.value!!) {
                for (j in 0 until gameModel.boardDimensions.value!!) {
                    if (board[i][j] != 0 && board[i][j] != gameModel.playerTurn.value?.id) {

                        var pos: Posicoes? = null
                        //Check Left
                        pos = searchBoardLine(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Left
                        pos = searchBoardDiagonalTop(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Top
                        pos = searchBoardColumn(i, j, true)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Top Right
                        pos = searchBoardDiagonalTop(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Right
                        pos = searchBoardLine(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Right
                        pos = searchBoardDiagonalBottom(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Bottom
                        pos = searchBoardColumn(i, j, false)
                        if (pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Left
                        pos = searchBoardDiagonalBottom(i, j, true)
                        if (pos != null)
                            k.add(pos)
                    }
                }
            }
            gameModel.playPositions.value = k
        }
    }

    /**
     * Função que verifica se o jogador pode inserir peça no local onde clicou
     */
    private fun checkIfPossible(line: Int, column: Int): Boolean {
        for (pos in gameModel.playPositions.value!!) {
            if (line == pos.linha && column == pos.coluna) {
                return true
            }
        }
        return false
    }

    /**
     * Função que, dada uma linha, procura um local para colocar uma peça
     */
    private fun searchBoardLine(line: Int, column: Int, checkingLeft: Boolean): Posicoes? {
        var col = column
        val board = gameModel.board.value!!

        while (if (checkingLeft) col < (gameModel.boardDimensions.value!! - 1) else col >= 1) {
            if (checkingLeft) {
                col++
            } else {
                col--
            }

            if (board[line][col] != 0 && board[line][col] == gameModel.playerTurn.value?.id) {
                if (checkingLeft) {
                    if (column - 1 >= 0 && board[line][column - 1] == 0) {
                        return Posicoes(line, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if (column + 1 < gameModel.boardDimensions.value!! && board[line][column + 1] == 0) {
                        return Posicoes(line, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que, dada uma coluna, procura um local para colocar uma peça
     */
    private fun searchBoardColumn(linha: Int, coluna: Int, checkingTop: Boolean): Posicoes? {
        var pos = linha
        val board = gameModel.board.value!!

        while (if (checkingTop) pos < (gameModel.boardDimensions.value!! - 1) else pos >= 1) {
            if (checkingTop) {
                pos++
            } else {
                pos--
            }

            if (board[pos][coluna] == gameModel.playerTurn.value?.id) {
                if (checkingTop) {
                    if ((linha - 1) >= 0 && board[linha - 1][coluna] == 0) {
                        return Posicoes(linha - 1, coluna)
                    } else {
                        return null
                    }
                } else {
                    if ((linha + 1) < gameModel.boardDimensions.value!! && board[linha + 1][coluna] == 0) {
                        return Posicoes(linha + 1, coluna)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que verifica o board para ver se é possivel colocar uma peça na diagonal
     * esquerda ou direita no topo da posição
     */
    private fun searchBoardDiagonalTop(line: Int, column: Int, checkingTopLeft: Boolean): Posicoes? {
        var lin = line
        var col = column
        val board = gameModel.board.value!!

        while (if (checkingTopLeft) (lin < gameModel.boardDimensions.value!! - 1) && (col < gameModel.boardDimensions.value!! - 1) else (lin < gameModel.boardDimensions.value!! - 1 && col >= 1)) {
            if (checkingTopLeft) {
                col++
            } else {
                col--
            }
            lin++

            if (board[lin][col] == gameModel.playerTurn.value?.id) {
                if (checkingTopLeft) {
                    if ((line - 1) >= 0 && (column - 1) >= 0 && board[line - 1][column - 1] == 0) {
                        return Posicoes(line - 1, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if ((line - 1 > 0 && column + 1 < gameModel.boardDimensions.value!!) && board[line - 1][column + 1] == 0) {
                        return Posicoes(line - 1, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que verifica o board para ver se é possivel colocar uma peça na diagonal
     * esquerda ou direita no fundo da posição
     */
    private fun searchBoardDiagonalBottom(line: Int, column: Int, checkingBottomLeft: Boolean): Posicoes? {
        var lin = line
        var col = column
        val board = gameModel.board.value!!

        while (if (checkingBottomLeft) (lin >= 1 && col < gameModel.boardDimensions.value!! - 1) else (lin >= 1 && col >= 1)) {
            if (checkingBottomLeft)
                col++
            else
                col--
            lin--

            if (board[lin][col] == gameModel.playerTurn.value?.id) {
                if (checkingBottomLeft) {
                    if ((column - 1 >= 0 && line + 1 < gameModel.boardDimensions.value!!) && board[line + 1][column - 1] == 0) {
                        return Posicoes(line + 1, column - 1)
                    } else {
                        return null
                    }
                } else {
                    if ((line + 1 < gameModel.boardDimensions.value!! && column + 1 < gameModel.boardDimensions.value!!) &&
                            board[line + 1][column + 1] == 0) {
                        return Posicoes(line + 1, column + 1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    /**
     * Função que vira todas as peças possíveis numa columa
     */
    private fun flipColumn(copyBoard: Array<IntArray>, line: Int, column: Int, flipTop: Boolean) {
        var lin = line
        while (if (flipTop) lin >= 1 else lin < gameModel.boardDimensions.value!! - 1) {
            if (flipTop) {
                lin--
            } else {
                lin++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][column] == 0)
                break

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[lin][column] == gameModel.playerTurn.value?.id) {
                while (if (flipTop) lin < line else lin > line) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipTop) {
                        lin++
                    } else {
                        lin--
                    }
                    copyBoard[lin][column] = gameModel.playerTurn.value?.id!!
                }
                break
            }
        }
    }

    /**
     * Função que vira todas as peças possíveis numa linha
     */
    private fun flipLine(copyBoard: Array<IntArray>, line: Int, column: Int, flipLeft: Boolean) {

        var col = column

        while (if (flipLeft) col >= 1 else col < gameModel.boardDimensions.value!! - 1) {

            if (flipLeft) {
                col--
            } else {
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[line][col] == 0) {
                break
            }

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[line][col] == gameModel.playerTurn.value?.id) {
                while (if (flipLeft) col < column else col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipLeft) {
                        col++
                    } else {
                        col--
                    }
                    copyBoard[line][col] = gameModel.playerTurn.value?.id!!
                }
                break
            }

        }
    }

    /**
     * Função que vira todas as peças possíveis em ambas as diagonais do topo
     */
    private fun flipTopDiagonal(copyBoard: Array<IntArray>, line: Int, column: Int, flipDiagonalLeft: Boolean) {
        var lin = line
        var col = column
        //Percorrer o board
        while (if (flipDiagonalLeft) lin >= 1 && col >= 1 else lin >= 1 && col < gameModel.boardDimensions.value!! - 1) {
            if (flipDiagonalLeft) {
                lin--
                col--
            } else {
                lin--
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][col] == 0)
                break

            //Se encontrar uma peça da pessoa que jogou
            if (copyBoard[lin][col] == gameModel.playerTurn.value?.id) {
                while (if (flipDiagonalLeft) lin < line && col < column else lin < line && col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipDiagonalLeft) {
                        lin++
                        col++
                    } else {
                        lin++
                        col--
                    }
                    copyBoard[lin][col] = gameModel.playerTurn.value?.id!!
                }
                break
            }

        }
    }

    /**
     * Função que vira todas as peças possíveis em ambas as diagonais do fundo
     */
    private fun flipDiagonalBottom(copyBoard: Array<IntArray>, line: Int, column: Int, flipDiagonalLeft: Boolean) {
        var lin = line
        var col = column
        while (if (flipDiagonalLeft) lin < gameModel.boardDimensions.value!! - 1 && col >= 1 else lin < gameModel.boardDimensions.value!! - 1 && col < gameModel.boardDimensions.value!! - 1) {
            if (flipDiagonalLeft) {
                lin++
                col--
            } else {
                lin++
                col++
            }

            //Se passar um "spot" vazio, não deve virar nenhuma peça logo deve sair
            if (copyBoard[lin][col] == 0)
                break

            if (copyBoard[lin][col] == gameModel.playerTurn.value?.id) {
                while (if (flipDiagonalLeft) lin > line && col < column else lin > line && col > column) {
                    //Volta na direção oposta até ao local inicial e,
                    //por cada posição que passa, altera a atual pela do jogador que jogou
                    if (flipDiagonalLeft) {
                        lin--
                        col++
                    } else {
                        lin--
                        col--
                    }
                    copyBoard[lin][col] = gameModel.playerTurn.value?.id!!
                }
                break
            }
        }
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
                         //   if (gameModel.playerTurn.value!!.photo != null) {
                         //       currentPlayer.put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(gameModel.playerTurn.value!!.photo!!))
                         //   }
                            jsonData.put(ConstStrings.CURRENT_PLAYER, currentPlayer)

                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }

                        ConstStrings.GAME_PASS_TURN -> gameModel.playerTurn.postValue(checkNextPlayer())

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
                            if (checkIfPossible(linha, coluna)) {
                                val playerId = gameModel.playerTurn.value!!.id
                                val player = checkNextPlayer()
                                updateValue(linha, coluna)
                                //Sends new position to all players + next player to play
                                for (i in gameModel.numJogadores.value!!) {
                                    if (i.socket != null) {
                                        jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PUT_NEW_PIECE)
                                        jsonData.put(ConstStrings.GAME_VALID_PIECE, true)

                                        val jsonArray = JSONArray()
                                        for (added in gameModel.addedPieces) {
                                            jsonArray.put(JSONObject().put(ConstStrings.BOARD_LINE, added.linha)
                                                    .put(ConstStrings.BOARD_COLUMN, added.coluna)
                                                    .put(ConstStrings.BOARD_POS_VALUE, added.value))
                                        }
                                        jsonData.put(ConstStrings.GAME_NEW_POSITIONS, jsonArray)

                                        val nextPlayer = JSONObject().put(ConstStrings.PLAYER_ID, player.id)
                                                .put(ConstStrings.PLAYER_NAME, player.name)
                                        //if (player.photo != null) {
                                        //    nextPlayer.put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(player.photo!!))
                                        //}

                                        jsonData.put(ConstStrings.GAME_PASS_TURN, nextPlayer)
                                    }
                                }
                            } else {
                                jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PUT_NEW_PIECE)
                                jsonData.put(ConstStrings.GAME_VALID_PIECE, false)
                            }

                            NetworkManager.sendInfo(socket, jsonData.toString())
                        }
                    }
                } catch (e: JSONException) {
                }
            }
        }
    }

    fun initComunication() {
        for (p in gameModel.numJogadores.value!!) {
            if (p.socket != null) {
                receiveInfoFromClients(p.socket!!)
            }
        }
    }


}