#include "main_window.h"

#include "loopback.h"

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent) {
    button = new QPushButton("click me", this);
    setCentralWidget(button);

    connect(button, &QPushButton::clicked, this, &MainWindow::onButtonClicked);

    setGeometry(100, 100, 400, 300);
}

MainWindow::~MainWindow() {
}

void MainWindow::onButtonClicked() {
    loopback("");
}
