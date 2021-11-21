package com.example.otello.game

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.otello.Posicoes

class GameViewModel : ViewModel(){

    val board = MutableLiveData<Array<IntArray>>()
    val playerTurn = MutableLiveData<Int>()
    val numJogadores = MutableLiveData<Int>()
    val boardDimensions = MutableLiveData<Int>()
    val playPositions = MutableLiveData<ArrayList<Posicoes>>()

    fun initBoard(boardSize : Int, boardDimen : Int) {
        //Inicia o Board com todas as posições vazias e guarda o num de colunas e linhas
        board.value = Array(boardSize) { IntArray(boardSize)}
        boardDimensions.value = boardDimen

        //Organize when PlayerNumber = 2
        board.value!![3][3] = 1
        board.value!![3][4] = 2
        board.value!![4][3] = 2
        board.value!![4][4] = 1
    }


    /**
     * Função que insere uma nova peça no tabuleiro
     */
    fun updateValue(line : Int, column : Int) {
        val copyBoard = board.value

        if(copyBoard != null && copyBoard[line][column] == 0){
            //Check if it's possible to put piece in that position
            if(checkIfPossible(line, column)) {
                //Coloca posição no board
                copyBoard[line][column] = playerTurn.value!!

                //Ver todas as peças e muda-las
                val newBoard = changePieces(line, column, copyBoard)

                //Atualizar o board
                board.postValue(newBoard)

                //Mudar de jogador
                changePlayer()
            }
        }
    }


    /**
     * Após um jogador inserir uma peça no tabuleiro, esta função é chamada para
     * alterar as peças já existentes no tabuleiro
     */
    fun changePieces(line: Int, column: Int, copyBoard : Array<IntArray>) : Array<IntArray>{
        var lin = line
        var col = column

        //Check Left
        while(col >= 1){
            col--

            if(copyBoard[line][col] == 0)
                break

            //Se encontrar peça do atual jogador, mudar todas até ao local selecionado
            if(copyBoard[line][col] == playerTurn.value){
                while(col < column){
                    col++
                    copyBoard[line][col] = playerTurn.value!!
                }
                break
            }

        }

        //Check Top Left Diagonal
        lin = line
        col = column
        while(lin >= 1 && col >= 1){
            lin--
            col--

            if(copyBoard[lin][col] == 0)
                break

            if(copyBoard[lin][col] == playerTurn.value){
                while(lin < line && col < column){
                    lin++
                    col++
                    copyBoard[lin][col] = playerTurn.value!!
                }
                break
            }

        }

        //Check Top
        lin = line
        while(lin >= 1){
            lin--

            if(copyBoard[lin][column] == 0)
                break

            if(copyBoard[lin][column] == playerTurn.value){
                while(lin < line){
                    lin++
                    copyBoard[lin][column] = playerTurn.value!!
                }
                break
            }
        }

        //check top right diagonal
        lin = line
        col = column
        while(lin >= 1 && col <= boardDimensions.value!! - 1){
            lin--
            col++

            if(copyBoard[lin][col] == 0)
                break

            if(copyBoard[lin][col] == playerTurn.value){
                while(lin < line && col > column){
                    lin++
                    col--
                    copyBoard[lin][col] = playerTurn.value!!
                }
                break
            }

        }

        //Check Right
        col = column
        while(col <= boardDimensions.value!! - 1){
            col++

            if(copyBoard[line][col] == 0)
                break

            //Se encontrar peça do atual jogador, mudar todas até ao local selecionado
            if(copyBoard[line][col] == playerTurn.value){
                while(col > column){
                    col--
                    copyBoard[line][col] = playerTurn.value!!
                }
                break
            }

        }

        //Check Bottom Right Diagonal
        lin = line
        col = column
        while(lin <= boardDimensions.value!! - 1 && col <= boardDimensions.value!! -1){
            lin++
            col++

            if(copyBoard[lin][col] == 0)
                break

            if(copyBoard[lin][col] == playerTurn.value){
                while(lin > line && col > column){
                    lin--
                    col--
                    copyBoard[lin][col] = playerTurn.value!!
                }
                break
            }

        }

        //Check Bottom
        lin = line

        while(lin <= boardDimensions.value!! - 1){
            lin++

            if(copyBoard[lin][column] == 0)
                break

            if(copyBoard[lin][column] == playerTurn.value){
                while(lin > line){
                    lin--
                    copyBoard[lin][column] = playerTurn.value!!
                }
                break
            }

        }

        //check bottom left diagonal
        lin = line
        col = column
        while(lin <= boardDimensions.value!! - 1 && col >= 1){
            lin++
            col--

            if(copyBoard[lin][col] == 0)
                break

            if(copyBoard[lin][col] == playerTurn.value){
                while(lin > line && col < column){
                    lin--
                    col++
                    copyBoard[lin][col] = playerTurn.value!!
                }
                break
            }

        }

        return copyBoard
    }


    /**
     * Esta função percorre o board para contar o numero de peças de cada jogador
     */
    fun checkPieces(){
        //TODO -> Ver o numero de peças...
    }

    /**
     * Esta função altera o jogador atual
     */
    fun changePlayer(player : Int = -1){

        if(player == -1) {
            if (playerTurn.value == numJogadores.value) {
                playerTurn.value = 1
            } else {
                playerTurn.value = playerTurn.value?.plus(1)
            }
        }
        else {
            playerTurn.value = player
        }

        getPossiblePositions()
    }

    /**
     * Baseado no jogador atual, esta função procura um local para o jogador jogar
     */

    fun getPossiblePositions(){
        val board = board.value

        if (board != null) {

            val k = arrayListOf<Posicoes>()

            for (i in board.indices){
                for (j in board[i].indices){
                    if(board[i][j] != 0 && board[i][j] != playerTurn.value){

                        var pos : Posicoes? = null
                        //Check Left
                        pos = searchBoardLine(i, j, true)
                        if(pos != null)
                            k.add(pos)

                        //Check Diagonal Top Left
                        pos = searchBoardDiagonalTop(i, j, true)
                        if(pos != null)
                            k.add(pos)

                        //Check Top
                        pos = searchBoardColumn(i, j, true)
                        if(pos != null)
                            k.add(pos)

                        //Check Diagonal Top Right
                        pos = searchBoardDiagonalTop(i, j, false)
                        if(pos != null)
                            k.add(pos)

                        //Check Right
                        pos = searchBoardLine(i, j, false)
                        if(pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Right
                        pos = searchBoardDiagonalBottom(i, j, false)
                        if(pos != null)
                            k.add(pos)

                        //Check Bottom
                        pos = searchBoardColumn(i, j, false)
                        if(pos != null)
                            k.add(pos)

                        //Check Diagonal Bottom Left
                        pos = searchBoardDiagonalBottom(i, j, true)
                        if(pos != null)
                            k.add(pos)
                    }
                }
            }
            playPositions.value = k
        }
    }

    /**
     * Função que verifica se o jogador pode inserir peça no local onde clicou
     */
    fun checkIfPossible(line: Int, column : Int) : Boolean {
        for (pos in playPositions.value!!){
            if(line == pos.linha && column == pos.coluna)
                return true

        }

        return false
    }

    /**
     * Função que, dada uma linha, procura um local para colocar uma peça
     */
    fun searchBoardLine(line : Int, column : Int, checkingLeft : Boolean) : Posicoes?{
        var col = column
        val board = board.value!!

        while(if(checkingLeft) col <= (boardDimensions.value!! - 1) else col >= 1){
            if(checkingLeft)
                col++
            else
                col--

            if(board[line][col] != 0 && board[line][col] == playerTurn.value){
                if(checkingLeft) {
                    if (column - 1 >= 0 && board[line][column - 1] == 0) {
                        return Posicoes(line, column - 1)
                    } else {
                        return null
                    }
                }
                else {
                    if (board[line][column + 1] == 0) {
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
    fun searchBoardColumn(linha : Int, coluna : Int, checkingTop : Boolean) : Posicoes?{

        var pos = linha
        val board = board.value!!

        while (if(checkingTop) pos <= (boardDimensions.value!! - 1) else pos >= 1){
            if(checkingTop)
                pos++
            else
                pos--

            if(board[pos][coluna] == playerTurn.value){

                if(checkingTop) {
                    if ((linha-1) >= 0 &&board[linha - 1][coluna] == 0) {
                        return Posicoes(linha-1, coluna)
                    } else {
                        return null
                    }
                }
                else {
                    if ( (linha+1) <= boardDimensions.value!! && board[linha+1][coluna] == 0) {
                        return Posicoes(linha+1, coluna)
                    } else {
                        return null
                    }
                }

            }

        }

        return null
    }


    fun searchBoardDiagonalTop(line : Int, column : Int, checkingTopLeft : Boolean) : Posicoes?{
        var lin = line
        var col = column
        val board = board.value!!

        while(if(checkingTopLeft) (lin <= boardDimensions.value!! - 1) && (col <= boardDimensions.value!! - 1) else (lin <= boardDimensions.value!! - 1 && col >= 1)){
            if (checkingTopLeft) {
                col++
            } else {
                col--
            }
            lin++

            if(board[lin][col] == playerTurn.value){
                if(checkingTopLeft){
                    if ( (line-1)>=0 && (column-1)>=0 && board[line - 1][column - 1] == 0) {
                        return Posicoes(line-1, column -1)
                    } else {
                        return null
                    }
                }
                else {
                    if ((line-1)>=0 && board[line - 1][column + 1] == 0) {
                        return Posicoes(line-1, column+1)
                    } else {
                        return null
                    }
                }
            }
        }
        return null
    }

    fun searchBoardDiagonalBottom(line: Int, column: Int, checkingBottomLeft : Boolean) : Posicoes?{
        var lin = line
        var col = column
        val board = board.value!!

        while(if(checkingBottomLeft) (lin >= 1 && col <= boardDimensions.value!! - 1) else (lin >= 1 && col >= 1)){
            if(checkingBottomLeft)
                col++
            else
                col--
            lin--

            if(board[lin][col] == playerTurn.value){
                if(checkingBottomLeft){
                    if (column - 1 >= 0 && board[line + 1][column - 1] == 0) {
                        return Posicoes(line+1, column-1)
                    } else {
                        return null
                    }
                }
                else {
                    if (board[line + 1][column + 1] == 0) {
                        return Posicoes(line+1, column+1)
                    } else {
                        return null
                    }
                }


            }

        }
        return null
    }

}