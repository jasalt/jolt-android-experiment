#include "raylib.h"

int main(int argc, char *argv[])
{
    (void)argc;
    (void)argv;

    InitWindow(0, 0, "Jolt Raylib plain C probe");
    SetTargetFPS(60);

    // Bounded for unattended validation, while long enough to exercise one
    // background/resume cycle before orderly NativeActivity return.
    for (int frame = 0; frame < 600 && !WindowShouldClose(); ++frame)
    {
        BeginDrawing();
        ClearBackground(RAYWHITE);
        DrawText("Jolt + Raylib Android", 24, 48, 28, DARKBLUE);
        DrawText("Plain C NativeActivity baseline", 24, 92, 20, DARKGRAY);
        DrawText(TextFormat("Frame: %d", frame), 24, 132, 20, MAROON);
        EndDrawing();
    }

    CloseWindow();
    return 0;
}
