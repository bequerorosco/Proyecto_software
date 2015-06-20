using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Speech.Recognition;

namespace reconocedorPalavbras
{
    public partial class Form1 : Form
    {

        private SpeechRecognitionEngine reconocedor = new SpeechRecognitionEngine();
        public Form1()
        {
            InitializeComponent();
        }


        private void button1_Click(object sender, EventArgs e)
        {

            reconocedor.SetInputToDefaultAudioDevice();
            reconocedor.LoadGrammar(new DictationGrammar());
            reconocedor.SpeechRecognized += new EventHandler<SpeechRecognizedEventArgs>(reconocedor_SpeechRecognized);
            reconocedor.RecognizeAsync(RecognizeMode.Multiple);
        }
        void reconocedor_SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {

            foreach (RecognizedWordUnit word in e.Result.Words)
            {

                   if (word.Text == "derecha")
                 {
                     this.pictureBox1.Location = new Point(pictureBox1.Location.X + 20, pictureBox1.Location.Y);
                    }
                   else if (word.Text == "izquierda")
                   {
                       this.pictureBox1.Location = new Point(pictureBox1.Location.X - 20, pictureBox1.Location.Y);
                   }

                   else if(word.Text == "abajo")
                   {
                       this.pictureBox1.Location = new Point(pictureBox1.Location.X , pictureBox1.Location.Y +20);
                   }
                   else if (word.Text == "arriba")
                   {
                       this.pictureBox1.Location = new Point(pictureBox1.Location.X, pictureBox1.Location.Y - 20);
                   }
                }
            }

        }

    }
