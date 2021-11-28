package com.example.otello.game.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.otello.game.model.GameModel
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes

class GameViewModel : ViewModel(){
    
    val gameModel = GameModel()

    fun initBoard(boardSize : Int, boardDimen : Int, numPlayers : Int) {
        //Inicia o Board com todas as posições vazias e guarda o num de colunas e linhas
        gameModel.board.value = Array(boardSize) { IntArray(boardSize)}
        gameModel.boardDimensions.value = boardDimen

        for(i in 0 until numPlayers)
            gameModel.numJogadores.value?.add(Jogador(i+1))

        //Organize board when numJogadores = 2
        if(gameModel.numJogadores.value?.size == 2) {
            gameModel.board.value!![3][3] = 1
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![4][3] = 2
            gameModel.board.value!![4][4] = 1
        } //Organize board when numJogadores = 3
        else if(gameModel.numJogadores.value?.size == 3){
            gameModel.board.value!![2][4] = 1
            gameModel.board.value!![2][5] = 2
            gameModel.board.value!![3][4] = 2
            gameModel.board.value!![3][5] = 1

            gameModel.board.value!![6][2] = 3
            gameModel.board.value!![6][2] = 1
            gameModel.board.value!![7][3] = 1
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
    fun updateValue(line : Int, column : Int) {
        val copyBoard = gameModel.board.value

        if(copyBoard != null && copyBoard[line][column] == 0){
            //Check if it's possible to put piece in that position
            if(checkIfPossible(line, column)) {
                //Coloca posição no board
                copyBoard[line][column] = gameModel.playerTurn.value?.id!!

                //Ver todas as peças e muda-las
                val newBoard : Array<IntArray>
                if(gameModel.bombMove){
                    newBoard = bombMove(copyBoard, line, column)
                    gameModel.playerTurn.value?.bombPiece = false
                    gameModel.bombMove = false
                }
                else{
                    newBoard = changePieces(line, column, copyBoard)
                }

                //Alterar as pontuações dos jogadores
                alterarPontuacoes(newBoard)

                //Atualizar o board
                gameModel.board.value = newBoard

                //Verificar se podemos continuar o jogo
                estadoJogo(newBoard)

                //Mudar de jogador
                changePlayer()
            }
        }
    }
    
    fun bombMove(board: Array<IntArray>, line : Int, column: Int) : Array<IntArray>{
        val copyBoard = board

        //Left
        if(column - 1 >= 0){
            copyBoard[line][column-1] = 0
        }

        //Diagonal Top Left
        if(line - 1 >= 0 && column-1 >= 0){
            copyBoard[line-1][column-1] = 0
        }
        //Top
        if(line - 1 >= 0){
            copyBoard[line-1][column] = 0
        }

        //Diagonal Top Right
        if(line - 1 >= 0 && column + 1 < gameModel.boardDimensions.value!!){
            copyBoard[line-1][column+1] = 0
        }

        //Right
        if(column + 1 < gameModel.boardDimensions.value!!){
            copyBoard[line][column+1] = 0
        }

        //Diagonal Bottom Right
        if(line + 1 < gameModel.boardDimensions.value!! && column + 1 < gameModel.boardDimensions.value!!){
            copyBoard[line+1][column+1] = 0
        }

        //Bottom
        if(line + 1 < gameModel.boardDimensions.value!!){
            copyBoard[line+1][column] = 0
        }

        //Diagonal Bottom Left
        if(line + 1 < gameModel.boardDimensions.value!! && column - 1 >= 0){
            copyBoard[line+1][column-1] = 0
        }

        return copyBoard
    }

    fun estadoJogo(board: Array<IntArray>){
        for(i in 0 until gameModel.boardDimensions.value!!){
            for(j in 0 until gameModel.boardDimensions.value!!){
                if(board[i][j] == 0){
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
    fun changePieces(line: Int, column: Int, copyBoard : Array<IntArray>) : Array<IntArray>{

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
    fun alterarPontuacoes(board : Array<IntArray>){
        val pont = arrayListOf(0,0,0)

        for (i in 0 until gameModel.boardDimensions.value!!) {
            for (j in 0 until gameModel.boardDimensions.value!!) {
                when (board[i][j]) {
                    1 -> pont[0]++
                    2 -> pont[1]++
                    3 -> pont[2]++
                }
            }
        }
        gameModel.pontuacaoPlayers.postValue(pont)

    }

    /**
     * Esta função altera o jogador atual e vê quais os locais onde ele pode jogar.
     */
    fun changePlayer(player: Int = -1) {
        if (player == -1) {
            if (gameModel.playerTurn.value?.id!! == gameModel.numJogadores.value?.size!!) {
                gameModel.playerTurn.value = gameModel.numJogadores.value?.get(0)
            } else {
                gameModel.playerTurn.value = gameModel.numJogadores.
                value?.get(gameModel.numJogadores.value?.indexOf(gameModel.playerTurn.value!!)?.plus(1)!!)
            }
        } else {
            gameModel.playerTurn.value = gameModel.numJogadores.value?.get(player-1)
        }

        getPossiblePositions()

        Log.i("TAG", "changePlayer: ")
    }

    /**
     * Baseado no jogador atual, esta função procura um local para o jogador jogar
     */
    private fun getPossiblePositions(){
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
            gameModel.playPositions.postValue(k)
        }
    }

    /**
     * Função que verifica se o jogador pode inserir peça no local onde clicou
     */
    private fun checkIfPossible(line: Int, column : Int) : Boolean {
        /*for (pos in gameModel.playPositions.value!!){
            if(line == pos.linha && column == pos.coluna){
                return true
            }
        }*/
        return true
    }

    /**
     * Função que, dada uma linha, procura um local para colocar uma peça
     */
    private fun searchBoardLine(line : Int, column : Int, checkingLeft : Boolean) : Posicoes?{
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
    private fun searchBoardDiagonalTop(line : Int, column : Int, checkingTopLeft : Boolean) : Posicoes?{
        var lin = line
        var col = column
        val board = gameModel.board.value!!

        while(if(checkingTopLeft) (lin < gameModel.boardDimensions.value!! - 1) && (col < gameModel.boardDimensions.value!! - 1) else (lin < gameModel.boardDimensions.value!! - 1 && col >= 1)){
            if (checkingTopLeft) {
                col++
            } else {
                col--
            }
            lin++

            if(board[lin][col] == gameModel.playerTurn.value?.id){
                if(checkingTopLeft){
                    if ( (line-1)>=0 && (column-1)>=0 && board[line - 1][column - 1] == 0) {
                        return Posicoes(line-1, column -1)
                    } else {
                        return null
                    }
                }
                else {
                    if ((line-1>0 && column + 1 < gameModel.boardDimensions.value!!) && board[line - 1][column + 1] == 0) {
                        return Posicoes(line-1, column+1)
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
    private fun searchBoardDiagonalBottom(line: Int, column: Int, checkingBottomLeft : Boolean) : Posicoes?{
        var lin = line
        var col = column
        val board = gameModel.board.value!!

        while(if(checkingBottomLeft) (lin >= 1 && col < gameModel.boardDimensions.value!! - 1) else (lin >= 1 && col >= 1)){
            if(checkingBottomLeft)
                col++
            else
                col--
            lin--

            if(board[lin][col] == gameModel.playerTurn.value?.id){
                if(checkingBottomLeft){
                    if ((column - 1 >= 0 && line + 1 < gameModel.boardDimensions.value!!) && board[line + 1][column - 1] == 0) {
                        return Posicoes(line+1, column-1)
                    } else {
                        return null
                    }
                }
                else {
                    if ((line + 1 < gameModel.boardDimensions.value!! && column +1 < gameModel.boardDimensions.value!! ) &&
                            board[line + 1][column + 1] == 0) {
                        return Posicoes(line+1, column+1)
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

}