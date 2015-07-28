library(shiny)

shinyUI(basicPage(
#   uiOutput("CreateUI")
  
  tabsetPanel(id="tab",
              tabPanel("서비스 조회",
                       ############### DB UI ###############
                       fixedPage(
                         uiOutput("ListUI")
                       )
              ),
              
              tabPanel("서비스 생성",
                       fixedPage(
                         uiOutput("CreateUI")
                       )
              )               
  )
  ))