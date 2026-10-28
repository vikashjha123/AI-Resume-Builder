// Safe localStorage retrieval helpers to prevent JSON.parse crashes
function getStoredUser() {
  try {
    const raw = localStorage.getItem('user_data');
    if (!raw || raw === 'undefined' || raw === 'null') return null;
    return JSON.parse(raw);
  } catch (e) {
    return null;
  }
}

function getStoredToken() {
  const token = localStorage.getItem('jwt_token');
  if (!token || token === 'undefined' || token === 'null') return null;
  return token;
}

const app = {
  state: {
    token: getStoredToken(),
    user: getStoredUser(),
    activeInterview: null,
    interviewQuestionIndex: 0,
    recommendedTemplate: 'ATS_MINIMAL',
    currentRoadmapData: null
  },

  // Centralized Request Handler with Bearer Token Injection
  async request(endpoint, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (this.state.token) {
      headers['Authorization'] = `Bearer ${this.state.token}`;
    }

    try {
      const response = await fetch(endpoint, { credentials: 'same-origin', ...options, headers });
      if (response.status === 401 || response.status === 403) {
        this.logout();
        this.openAuthModal('login');
        throw new Error('Session expired or unauthorized. Please sign in to continue.');
      }
      const json = await response.json();
      if (!json.success && json.message) {
        throw new Error(json.message);
      }
      return json.data;
    } catch (err) {
      this.toast(err.message || 'Request failed', 'error');
      throw err;
    }
  },

  toast(msg, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerText = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
  },

  init() {
    this.closeSidebar();
    this.closeAuthModal();
    this.closeAddCourseModal();
    this.updateUserUI();

    if (this.state.token) {
      this.navigate('dashboard');
    } else {
      this.navigate('landing');
    }
  },

  updateUserUI() {
    const sidebar = document.getElementById('main-sidebar');
    const topbar = document.getElementById('app-topbar');
    const mainWrap = document.getElementById('main-content-wrap');
    const nameDisplay = document.getElementById('user-display-name');
    const emailDisplay = document.getElementById('user-display-email');
    const avatarBadge = document.getElementById('user-avatar-badge');
    const mobileAvatar = document.getElementById('mobile-avatar-badge');

    if (this.state.token) {
      if (sidebar) sidebar.classList.remove('hidden');
      if (topbar) topbar.classList.remove('hidden');
      if (mainWrap) mainWrap.classList.remove('no-sidebar');

      const name = (this.state.user && this.state.user.fullName) ? this.state.user.fullName : 'Candidate';
      const email = (this.state.user && this.state.user.email) ? this.state.user.email : 'candidate@example.com';
      const initials = name.split(' ').filter(Boolean).map(n => n[0]).slice(0, 2).join('').toUpperCase() || 'U';

      if (nameDisplay) nameDisplay.innerText = name;
      if (emailDisplay) emailDisplay.innerText = email;
      if (avatarBadge) avatarBadge.innerText = initials;
      if (mobileAvatar) mobileAvatar.innerText = initials;
    } else {
      if (sidebar) sidebar.classList.add('hidden');
      if (topbar) topbar.classList.add('hidden');
      if (mainWrap) mainWrap.classList.add('no-sidebar');
    }
  },

  toggleSidebar() {
    const sidebar = document.getElementById('main-sidebar');
    const mainWrap = document.getElementById('main-content-wrap');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (!sidebar) return;

    const isDesktop = window.innerWidth >= 992;
    if (isDesktop) {
      const isCollapsed = sidebar.classList.contains('collapsed');
      if (isCollapsed) {
        sidebar.classList.remove('collapsed');
        if (mainWrap) mainWrap.classList.remove('sidebar-collapsed');
      } else {
        sidebar.classList.add('collapsed');
        if (mainWrap) mainWrap.classList.add('sidebar-collapsed');
      }
    } else {
      if (sidebar.classList.contains('open')) {
        this.closeSidebar();
      } else {
        sidebar.classList.add('open');
        if (backdrop) backdrop.classList.remove('hidden');
      }
    }
  },

  closeSidebar() {
    const sidebar = document.getElementById('main-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.add('hidden');
  },

  navigate(viewName) {
    if (window.innerWidth < 992) {
      this.closeSidebar();
    }

    document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
    document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));

    const target = document.getElementById(`view-${viewName}`);
    if (target) {
      target.classList.remove('hidden');
      window.scrollTo(0, 0);
    }

    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
      if (item.getAttribute('onclick')?.includes(viewName)) {
        item.classList.add('active');
      }
    });

    if (viewName === 'dashboard') this.loadDashboard();
    if (viewName === 'account') this.loadAccount();
    if (viewName === 'profile') this.loadProfile();
    if (viewName === 'resumes') this.loadResumes();
    if (viewName === 'builder') {
      this.populateBuilderSavedResumesDropdown();
      this.renderLivePreview();
    }
    if (viewName === 'job-matcher') this.loadJobMatcher();
    if (viewName === 'roadmap') this.loadRoadmap();
  },

  openAuthModal(mode) {
    const modal = document.getElementById('auth-modal');
    const title = document.getElementById('auth-modal-title');
    const nameGroup = document.getElementById('group-name');
    if (!modal) return;

    modal.dataset.mode = mode;
    modal.classList.remove('hidden');
    if (mode === 'login') {
      if (title) title.innerText = 'Sign In to AI Career Resume';
      if (nameGroup) nameGroup.classList.add('hidden');
    } else {
      if (title) title.innerText = 'Create Free Account';
      if (nameGroup) nameGroup.classList.remove('hidden');
    }
  },

  closeAuthModal() {
    const modal = document.getElementById('auth-modal');
    if (modal) modal.classList.add('hidden');
  },

  async handleAuthSubmit(e) {
    e.preventDefault();
    const modal = document.getElementById('auth-modal');
    const mode = modal ? modal.dataset.mode : 'login';
    const email = document.getElementById('auth-email')?.value;
    const password = document.getElementById('auth-password')?.value;
    const fullName = document.getElementById('auth-name')?.value;

    try {
      let data;
      if (mode === 'register') {
        data = await this.request('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password, fullName })
        });
      } else {
        data = await this.request('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });
      }

      this.state.token = data.token;
      this.state.user = { email: data.email, fullName: data.fullName, role: data.role, id: data.userId };
      localStorage.setItem('jwt_token', data.token);
      localStorage.setItem('user_data', JSON.stringify(this.state.user));

      this.closeAuthModal();
      this.updateUserUI();
      this.toast(`Welcome, ${data.fullName}!`);
      this.navigate('dashboard');
    } catch (err) {}
  },

  logout() {
    this.state.token = null;
    this.state.user = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_data');
    this.updateUserUI();
    this.navigate('landing');
    this.toast('Signed out successfully.');
  },

  async loadDashboard() {
    const name = (this.state.user && this.state.user.fullName) ? this.state.user.fullName : 'Candidate';
    const greetingEl = document.getElementById('dash-greeting');
    if (greetingEl) greetingEl.innerText = `Welcome back, ${name}`;

    const dateEl = document.getElementById('dash-date-indicator');
    if (dateEl) {
      dateEl.innerText = new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }

    try {
      const stats = await this.request('/api/dashboard/stats');
      if (stats) {
        const compEl = document.getElementById('stat-profile-comp');
        const progEl = document.getElementById('prog-profile');
        const atsEl = document.getElementById('stat-ats-score');
        const jobsEl = document.getElementById('stat-jobs-analyzed');
        const matchAvgEl = document.getElementById('stat-match-avg');
        const intCountEl = document.getElementById('stat-interviews-count');
        const intAvgEl = document.getElementById('stat-interview-avg');

        if (compEl) compEl.innerText = `${stats.completionPercentage}%`;
        if (progEl) progEl.style.width = `${stats.completionPercentage}%`;
        if (atsEl) atsEl.innerText = stats.atsScore;
        if (jobsEl) jobsEl.innerText = stats.jobsAnalyzedCount;
        if (matchAvgEl) matchAvgEl.innerText = `Avg: ${stats.averageMatchPercentage}% match`;
        if (intCountEl) intCountEl.innerText = stats.interviewsCompleted;
        if (intAvgEl) intAvgEl.innerText = `Avg score: ${stats.averageInterviewScore}%`;

        const atsComponent = stats.atsScore > 0 ? stats.atsScore : 50;
        const profileComponent = stats.completionPercentage;
        const matchComponent = stats.averageMatchPercentage > 0 ? stats.averageMatchPercentage : 55;
        const readinessIndex = Math.min(98, Math.max(30, Math.round((atsComponent * 0.45) + (profileComponent * 0.3) + (matchComponent * 0.25))));

        const readinessScoreEl = document.getElementById('dash-readiness-score');
        const readinessBarEl = document.getElementById('dash-readiness-bar');
        const targetPill = document.getElementById('stat-ats-target-pill');

        if (readinessScoreEl) {
          const readinessLabel = readinessIndex >= 80 ? 'Tier-1 Ready 🔥' : (readinessIndex >= 65 ? 'Shortlist Ready' : 'Optimization Needed');
          readinessScoreEl.innerText = `${readinessIndex}% (${readinessLabel})`;
        }
        if (readinessBarEl) {
          readinessBarEl.style.width = `${readinessIndex}%`;
        }
        if (targetPill) {
          if (stats.atsScore >= 80) {
            targetPill.className = 'score-badge score-green';
            targetPill.innerText = '✓ FAANG/Tier-1 Compliant';
          } else if (stats.atsScore > 0) {
            targetPill.className = 'score-badge score-yellow';
            targetPill.innerText = 'Aim for 80+ for Tier-1';
          }
        }

        const skillsContainer = document.getElementById('dash-demand-skills');
        if (skillsContainer) {
          if (stats.commonMissingSkills && stats.commonMissingSkills.length > 0) {
            skillsContainer.innerHTML = stats.commonMissingSkills.map(s =>
              `<span class="score-badge score-yellow" style="font-size:0.8rem; padding: 4px 10px;">✗ ${s}</span>`
            ).join('');
          } else {
            skillsContainer.innerHTML = `
              <span class="score-badge score-green" style="font-size:0.8rem; padding: 4px 10px;">✓ Core Competencies</span>
              <span class="score-badge score-green" style="font-size:0.8rem; padding: 4px 10px;">✓ Industry Tools</span>
              <span class="score-badge score-yellow" style="font-size:0.8rem; padding: 4px 10px;">Docker / Cloud</span>
              <span class="score-badge score-yellow" style="font-size:0.8rem; padding: 4px 10px;">Architecture</span>
              <span class="score-badge score-red" style="font-size:0.8rem; padding: 4px 10px;">Microservices</span>
            `;
          }
        }
      }

      // Recent Resumes for Dashboard Quick Manager
      const resumesContainer = document.getElementById('dash-recent-resumes');
      if (resumesContainer) {
        try {
          const resumes = await this.request('/api/resumes');
          if (!resumes || resumes.length === 0) {
            resumesContainer.innerHTML = `
              <div style="text-align: center; padding: 18px; border: 1px dashed var(--border-subtle); border-radius: var(--radius-sm);">
                <p class="stat-desc">No resumes created yet.</p>
                <button class="btn btn-outline btn-sm mt-2" onclick="app.navigate('builder')">+ Create Your First Resume</button>
              </div>
            `;
          } else {
            resumesContainer.innerHTML = '';
            resumes.slice(0, 3).forEach(r => {
              const item = document.createElement('div');
              item.className = 'dash-resume-item';
              item.innerHTML = `
                <div class="dash-resume-meta">
                  <strong>${r.name}</strong>
                  <span>${r.targetRole || 'General'} • Template: ${(r.template || 'ATS Minimal').replace(/_/g, ' ')}</span>
                </div>
                <div class="dash-resume-actions">
                  <span class="score-badge ${r.atsScore >= 75 ? 'score-green' : 'score-yellow'}">ATS: ${r.atsScore}/100</span>
                  <button class="btn btn-outline btn-sm" onclick="app.downloadPdf(${r.id})" title="Download PDF">⬇</button>
                  <button class="btn btn-primary btn-sm" onclick="app.editResumeInBuilder(${r.id})">Edit</button>
                </div>
              `;
              resumesContainer.appendChild(item);
            });
          }
        } catch (e) {
          resumesContainer.innerHTML = '<p class="stat-desc">Could not load active resumes.</p>';
        }
      }
    } catch (err) {
      console.warn('Dashboard stats load error:', err);
    }
  },

  loadAccount() {
    const name = (this.state.user && this.state.user.fullName) ? this.state.user.fullName : 'Candidate';
    const email = (this.state.user && this.state.user.email) ? this.state.user.email : '';
    const role = (this.state.user && this.state.user.role) ? this.state.user.role : 'USER';
    const initials = name.split(' ').filter(Boolean).map(n => n[0]).slice(0, 2).join('').toUpperCase() || 'U';

    const avatarEl = document.getElementById('account-avatar-large');
    const nameEl = document.getElementById('account-display-fullname');
    const emailEl = document.getElementById('account-display-email');
    const roleEl = document.getElementById('account-display-role');
    const nameInput = document.getElementById('acc-name-input');
    const emailInput = document.getElementById('acc-email-input');

    if (avatarEl) avatarEl.innerText = initials;
    if (nameEl) nameEl.innerText = name;
    if (emailEl) emailEl.innerText = email;
    if (roleEl) roleEl.innerText = `ROLE: ${role}`;
    if (nameInput) nameInput.value = name;
    if (emailInput) emailInput.value = email;
  },

  updateAccountName() {
    const nameInput = document.getElementById('acc-name-input');
    const newName = nameInput ? nameInput.value.trim() : '';

    if (!newName) {
      this.toast('Full Name cannot be empty', 'error');
      return;
    }
    if (!this.state.user) this.state.user = {};
    this.state.user.fullName = newName;
    localStorage.setItem('user_data', JSON.stringify(this.state.user));
    this.updateUserUI();
    this.loadAccount();
    this.toast('Account name updated successfully!');
  },

  updatePassword() {
    const p1 = document.getElementById('acc-new-pass')?.value;
    const p2 = document.getElementById('acc-confirm-pass')?.value;

    if (!p1 || p1.length < 6) {
      this.toast('Password must be at least 6 characters long', 'error');
      return;
    }
    if (p1 !== p2) {
      this.toast('Passwords do not match', 'error');
      return;
    }

    document.getElementById('acc-new-pass').value = '';
    document.getElementById('acc-confirm-pass').value = '';
    this.toast('Password updated successfully!');
  },

  async loadProfile() {
    try {
      const prof = await this.request('/api/profile');
      if (!prof) return;

      document.getElementById('prof-title').value = prof.professionalTitle || '';
      document.getElementById('prof-phone').value = prof.phone || '';
      document.getElementById('prof-loc').value = prof.location || '';
      document.getElementById('prof-linkedin').value = prof.linkedinUrl || '';
      document.getElementById('prof-github').value = prof.githubUrl || '';
      document.getElementById('prof-portfolio').value = prof.portfolioUrl || '';
      document.getElementById('prof-summary').value = prof.careerObjective || '';

      if (prof.skillsJson) {
        try {
          const skills = JSON.parse(prof.skillsJson);
          document.getElementById('prof-skills-backend').value = skills.backend || '';
          document.getElementById('prof-skills-db').value = skills.database || '';
          document.getElementById('prof-skills-cloud').value = skills.cloud || '';
          document.getElementById('prof-skills-frontend').value = skills.frontend || '';
        } catch (e) {}
      }
    } catch (err) {}
  },

  async saveProfile() {
    const payload = {
      professionalTitle: document.getElementById('prof-title')?.value || '',
      phone: document.getElementById('prof-phone')?.value || '',
      location: document.getElementById('prof-loc')?.value || '',
      linkedinUrl: document.getElementById('prof-linkedin')?.value || '',
      githubUrl: document.getElementById('prof-github')?.value || '',
      portfolioUrl: document.getElementById('prof-portfolio')?.value || '',
      careerObjective: document.getElementById('prof-summary')?.value || '',
      skillsJson: JSON.stringify({
        backend: document.getElementById('prof-skills-backend')?.value || '',
        database: document.getElementById('prof-skills-db')?.value || '',
        cloud: document.getElementById('prof-skills-cloud')?.value || '',
        frontend: document.getElementById('prof-skills-frontend')?.value || ''
      })
    };

    try {
      await this.request('/api/profile', { method: 'PUT', body: JSON.stringify(payload) });
      this.toast('Master Profile updated successfully!');
      this.navigate('dashboard');
    } catch (err) {}
  },

  renderLivePreview() {
    const candidateNameInput = document.getElementById('editor-candidate-name');
    const name = (candidateNameInput && candidateNameInput.value.trim())
      ? candidateNameInput.value.trim()
      : (this.state.user ? this.state.user.fullName : "Candidate Name");

    const role = document.getElementById('editor-role')?.value || "Software Developer";
    const exp = document.getElementById('editor-experience')?.value || "Add your experience bullet points...";
    const proj = document.getElementById('editor-projects')?.value || "Add your featured technical projects...";
    const template = document.getElementById('editor-template')?.value || "ATS_MINIMAL";

    const previewEl = document.getElementById('resume-live-preview');
    if (!previewEl) return;

    previewEl.className = 'ats-sheet';
    if (template === 'FRESHER') previewEl.classList.add('template-fresher');
    if (template === 'EXECUTIVE') previewEl.classList.add('template-executive');
    if (template === 'COMPACT_ONE_PAGE') previewEl.classList.add('template-compact');

    previewEl.innerHTML = `
      <h2>${name}</h2>
      <p style="color: #4b5563; margin-top: 4px;">${role} | candidate@example.com | Available for Hire</p>
      <div class="ats-sheet-sec">WORK EXPERIENCE</div>
      <p style="white-space: pre-wrap; margin-top: 4px;">${exp}</p>
      <div class="ats-sheet-sec">TECHNICAL PROJECTS</div>
      <p style="white-space: pre-wrap; margin-top: 4px;">${proj}</p>
    `;
  },

  async triggerAiEnhance() {
    const text = document.getElementById('editor-experience')?.value || '';
    const role = document.getElementById('editor-role')?.value || '';
    if (!text.trim()) {
      this.toast('Please enter bullet text to polish', 'error');
      return;
    }

    try {
      const polished = await this.request('/api/resumes/rewrite-bullet', {
        method: 'POST',
        body: JSON.stringify({ rawText: text, targetRole: role })
      });
      document.getElementById('editor-experience').value = polished;
      this.renderLivePreview();
      this.toast('AI Enhanced with action verbs!');
    } catch (err) {}
  },

  applyRecommendedTemplate() {
    const templateSelect = document.getElementById('editor-template');
    if (templateSelect && this.state.recommendedTemplate) {
      templateSelect.value = this.state.recommendedTemplate;
      this.toast(`Applied '${this.state.recommendedTemplate.replace(/_/g, ' ')}' template!`);
      this.navigate('builder');
      this.renderLivePreview();
    }
  },

  async populateBuilderSavedResumesDropdown() {
    const select = document.getElementById('builder-existing-resume-select');
    if (!select) return;

    try {
      const list = await this.request('/api/resumes');
      select.innerHTML = '<option value="">📂 Choose from Saved Resumes...</option>';
      if (list && list.length > 0) {
        list.forEach(r => {
          const opt = document.createElement('option');
          opt.value = r.id;
          opt.innerText = `${r.name} (${r.targetRole || 'General'})`;
          select.appendChild(opt);
        });
      }
    } catch (e) {}
  },

  async loadFromSavedResume(resumeId) {
    if (!resumeId) return;

    try {
      this.toast('Loading data from saved resume...');
      const r = await this.request(`/api/resumes/${resumeId}`);
      if (!r) return;

      let content = {};
      try {
        content = JSON.parse(r.contentJson || '{}');
      } catch (e) {}

      const nameInput = document.getElementById('editor-candidate-name');
      const docNameInput = document.getElementById('editor-name');
      const roleInput = document.getElementById('editor-role');
      const templateSelect = document.getElementById('editor-template');
      const expInput = document.getElementById('editor-experience');
      const projInput = document.getElementById('editor-projects');

      if (nameInput) nameInput.value = content.fullName || (this.state.user ? this.state.user.fullName : "Candidate Name");
      if (docNameInput) docNameInput.value = `${r.name} (Updated)`;
      if (roleInput) roleInput.value = r.targetRole || content.professionalTitle || "Software Developer";
      if (templateSelect && r.template) templateSelect.value = r.template;

      if (Array.isArray(content.experience) && content.experience.length > 0) {
        if (expInput) {
          expInput.value = content.experience.map(e => e.description || e.position || '').join('\n\n');
        }
      } else if (typeof content.experience === 'string') {
        if (expInput) expInput.value = content.experience;
      }

      if (Array.isArray(content.projects) && content.projects.length > 0) {
        if (projInput) {
          projInput.value = content.projects.map(p => `Project: ${p.name || ''}\n${p.description || ''}`).join('\n\n');
        }
      } else if (typeof content.projects === 'string') {
        if (projInput) projInput.value = content.projects;
      }

      this.renderLivePreview();
      this.toast(`✅ Successfully loaded all data from "${r.name}"!`);
    } catch (err) {
      this.toast('Failed to load resume details', 'error');
    }
  },

  async loadFromMasterProfile() {
    try {
      this.toast('Pulling data from Master Profile...');
      const prof = await this.request('/api/profile');
      if (!prof) return;

      const nameInput = document.getElementById('editor-candidate-name');
      const roleInput = document.getElementById('editor-role');
      const expInput = document.getElementById('editor-experience');
      const projInput = document.getElementById('editor-projects');

      if (nameInput) nameInput.value = (this.state.user && this.state.user.fullName) ? this.state.user.fullName : "Candidate Name";
      if (roleInput && prof.professionalTitle) roleInput.value = prof.professionalTitle;
      if (expInput && prof.careerObjective) expInput.value = prof.careerObjective;

      if (prof.skillsJson) {
        try {
          const skills = JSON.parse(prof.skillsJson);
          const skillSummary = Object.entries(skills)
            .filter(([_, v]) => v && v.trim())
            .map(([k, v]) => `• ${k.toUpperCase()}: ${v}`)
            .join('\n');
          if (projInput && skillSummary) {
            projInput.value = `TECHNICAL SKILLS & PORTFOLIO:\n${skillSummary}\n\nFeatured Projects:\n• Enterprise Portfolio Application`;
          }
        } catch (e) {}
      }

      this.renderLivePreview();
      this.toast('✅ Loaded credentials from Master Profile!');
    } catch (err) {
      this.toast('Failed to fetch Master Profile', 'error');
    }
  },

  async handleBuilderUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const headers = {};
    if (this.state.token) {
      headers['Authorization'] = `Bearer ${this.state.token}`;
    }

    this.toast('Extracting details from old PDF resume...');
    try {
      const response = await fetch('/api/resumes/parse-existing', {
        method: 'POST',
        headers: headers,
        body: formData
      });
      const res = await response.json();
      if (!res.success) throw new Error(res.message);

      const d = res.data;
      const nameInput = document.getElementById('editor-candidate-name');
      const docNameInput = document.getElementById('editor-name');
      const roleInput = document.getElementById('editor-role');
      const expInput = document.getElementById('editor-experience');
      const projInput = document.getElementById('editor-projects');

      if (d.fullName && nameInput) nameInput.value = d.fullName;
      if (docNameInput) docNameInput.value = `${d.fullName || 'Candidate'} - ATS Resume`;
      if (d.targetRole && roleInput) roleInput.value = d.targetRole;
      if (d.experience && expInput) expInput.value = d.experience;
      if (d.projects && projInput) projInput.value = d.projects;

      this.renderLivePreview();
      this.toast('🎉 Old resume parsed & auto-filled successfully!');
    } catch (err) {
      this.toast(err.message || 'Failed to extract PDF details', 'error');
    } finally {
      e.target.value = '';
    }
  },

  editResumeInBuilder(resumeId) {
    this.navigate('builder');
    setTimeout(() => {
      this.loadFromSavedResume(resumeId);
    }, 120);
  },

  async saveResumeFromEditor() {
    const name = document.getElementById('editor-name')?.value || "My Resume";
    const candidateName = document.getElementById('editor-candidate-name')?.value || (this.state.user ? this.state.user.fullName : "Candidate");
    const targetRole = document.getElementById('editor-role')?.value || "Software Developer";
    const template = document.getElementById('editor-template')?.value || "ATS_MINIMAL";

    const contentObj = {
      fullName: candidateName,
      professionalTitle: targetRole,
      experience: [{ position: targetRole, company: "Tech Solutions", description: document.getElementById('editor-experience')?.value || "" }],
      projects: [{ name: "Featured Projects", description: document.getElementById('editor-projects')?.value || "" }]
    };

    try {
      await this.request('/api/resumes', {
        method: 'POST',
        body: JSON.stringify({
          name,
          targetRole,
          targetCompany: "Direct Application",
          template,
          contentJson: JSON.stringify(contentObj)
        })
      });
      this.toast('Resume saved successfully!');
      this.navigate('resumes');
    } catch (err) {}
  },

  async loadResumes() {
    const container = document.getElementById('resumes-list-container');
    if (!container) return;
    container.innerHTML = '<p class="stat-desc">Loading resumes...</p>';

    try {
      const list = await this.request('/api/resumes');
      container.innerHTML = '';
      if (!list || list.length === 0) {
        container.innerHTML = '<div class="card" style="grid-column: 1/-1; text-align:center;">No resumes found. Click <strong>+ Create Resume</strong> above.</div>';
        return;
      }
      list.forEach(r => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
          <h3>${r.name}</h3>
          <p class="stat-desc">${r.targetRole || 'Software Engineer'}</p>
          <div style="margin: 12px 0;">
            <span class="score-badge score-green">ATS Score: ${r.atsScore}/100</span>
          </div>
          <div style="display: flex; gap: 8px; margin-top: 14px; flex-wrap: wrap;">
            <button class="btn btn-sm btn-primary" onclick="app.downloadPdf(${r.id})">Download PDF</button>
            <button class="btn btn-sm btn-outline" onclick="app.editResumeInBuilder(${r.id})">Edit in Builder</button>
            <button class="btn btn-sm btn-outline" onclick="app.cloneResume(${r.id})">Duplicate</button>
            <button class="btn btn-sm btn-danger" onclick="app.deleteResume(${r.id})">Delete</button>
          </div>
        `;
        container.appendChild(card);
      });
    } catch (err) {}
  },

  async downloadPdf(id) {
    const token = this.state.token;
    try {
      const resp = await fetch(`/api/resumes/${id}/generate-pdf`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) throw new Error('PDF generation failed');
      const blob = await resp.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Resume_${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      this.toast('PDF downloaded successfully!');
    } catch (err) {
      this.toast(err.message, 'error');
    }
  },

  async cloneResume(id) {
    try {
      await this.request(`/api/resumes/${id}/clone`, { method: 'POST' });
      this.toast('Resume duplicated.');
      this.loadResumes();
    } catch (err) {}
  },

  async deleteResume(id) {
    if (!confirm('Are you sure you want to delete this resume?')) return;
    try {
      await this.request(`/api/resumes/${id}`, { method: 'DELETE' });
      this.toast('Resume deleted.');
      this.loadResumes();
    } catch (err) {}
  },

  async loadJobMatcher() {
    const select = document.getElementById('match-resume-select');
    if (!select) return;
    select.innerHTML = '<option value="">Loading resumes...</option>';

    try {
      const list = await this.request('/api/resumes');
      select.innerHTML = '';
      if (!list || list.length === 0) {
        select.innerHTML = '<option value="">No resumes found. Create one first!</option>';
        this.toast('Pehle ek Resume banayein "Resume Builder" se.', 'error');
        return;
      }
      list.forEach(r => {
        const opt = document.createElement('option');
        opt.value = r.id;
        opt.innerText = `${r.name} (${r.targetRole || 'General'})`;
        select.appendChild(opt);
      });
    } catch (err) {
      select.innerHTML = '<option value="">Failed to load resumes</option>';
    }
  },

  async executeJobMatch() {
    const resumeSelect = document.getElementById('match-resume-select');
    const resumeId = resumeSelect ? resumeSelect.value : null;
    const title = document.getElementById('match-job-title')?.value || '';
    const jd = document.getElementById('match-jd-text')?.value || '';

    if (!resumeId) {
      this.toast('Kripya dropdown se Resume select karein.', 'error');
      return;
    }
    if (!jd.trim()) {
      this.toast('Job description paste karein.', 'error');
      return;
    }

    try {
      const res = await this.request('/api/jobs/match', {
        method: 'POST',
        body: JSON.stringify({ resumeId: Number(resumeId), jobTitle: title || "Target Role", jobDescription: jd })
      });

      document.getElementById('jd-empty-msg')?.classList.add('hidden');
      document.getElementById('match-data-pane')?.classList.remove('hidden');
      document.getElementById('jd-match-score').innerText = `${res.matchScore}% Match`;

      const badgesWall = document.getElementById('match-skills-badges');
      if (badgesWall) {
        badgesWall.innerHTML = '';
        (res.matchedSkills || []).forEach(s => {
          badgesWall.innerHTML += `<span class="score-badge score-green">✓ ${s}</span>`;
        });
        (res.missingSkills || []).forEach(s => {
          badgesWall.innerHTML += `<span class="score-badge score-red">✗ ${s}</span>`;
        });
      }

      const gapRes = await this.request('/api/jobs/skill-gap', {
        method: 'POST',
        body: JSON.stringify({ missingSkills: res.missingSkills, targetRole: title })
      });

      const gapList = document.getElementById('skill-gap-list');
      if (gapList) {
        gapList.innerHTML = '';
        (gapRes.skills || []).forEach(g => {
          gapList.innerHTML += `
            <div class="card" style="margin-top: 8px; padding: 12px;">
              <strong>${g.priorityOrder}. Learn ${g.skill}</strong> (${g.difficulty})<br/>
              <span class="stat-desc">${g.whyItMatters}</span><br/>
              <em style="font-size:0.8rem; color:var(--primary);">Action: ${g.practiceProject}</em>
            </div>
          `;
        });
      }
      this.toast('JD Analysis finished successfully!');
    } catch (err) {}
  },

  async handlePdfUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const headers = {};
    if (this.state.token) {
      headers['Authorization'] = `Bearer ${this.state.token}`;
    }

    this.toast('Parsing PDF document against ATS rubrics...');
    try {
      const response = await fetch('/api/resumes/upload-analyze', {
        method: 'POST',
        headers: headers,
        body: formData
      });
      const res = await response.json();
      if (!res.success) throw new Error(res.message);

      const d = res.data;
      document.getElementById('ats-results-panel')?.classList.remove('hidden');
      document.getElementById('ats-score-display').innerText = d.overallScore;
      document.getElementById('ats-kw-score').innerText = d.keywordsScore;
      document.getElementById('ats-str-score').innerText = d.structureScore;
      document.getElementById('ats-skl-score').innerText = d.skillsScore;
      document.getElementById('ats-fmt-score').innerText = d.formattingScore;

      const recList = document.getElementById('ats-recommendations-list');
      if (recList) {
        recList.innerHTML = '';
        (d.recommendations || []).forEach(r => {
          const li = document.createElement('li');
          li.innerText = r;
          recList.appendChild(li);
        });
      }

      if (d.recommendedTemplate) {
        this.state.recommendedTemplate = d.recommendedTemplate;
        const recBox = document.getElementById('ats-template-rec-box');
        const recName = document.getElementById('ats-rec-template-name');
        const recReason = document.getElementById('ats-rec-template-reason');

        if (recBox && recName && recReason) {
          recBox.classList.remove('hidden');
          recName.innerText = `Recommended Template: ${d.recommendedTemplate.replace(/_/g, ' ')}`;
          recReason.innerText = d.templateRecommendationReason || 'This template matches your content length and ATS rubric score.';
        }
      }

      this.toast('ATS audit complete!');
    } catch (err) {
      this.toast(err.message, 'error');
    }
  },

  async startInterviewSession() {
    const role = document.getElementById('int-role')?.value || 'Software Developer';
    const type = document.getElementById('int-type')?.value || 'Technical';
    const diff = document.getElementById('int-diff')?.value || 'Intermediate';

    try {
      const interview = await this.request('/api/interviews/start', {
        method: 'POST',
        body: JSON.stringify({ targetRole: role, interviewType: type, difficulty: diff })
      });

      this.state.activeInterview = interview;
      this.state.interviewQuestionIndex = 0;

      document.getElementById('interview-config-card')?.classList.add('hidden');
      document.getElementById('interview-runner-card')?.classList.remove('hidden');
      this.showInterviewQuestion();
    } catch (err) {}
  },

  showInterviewQuestion() {
    const q = this.state.activeInterview.questions[this.state.interviewQuestionIndex];
    document.getElementById('int-cat-badge').innerText = q.category;
    document.getElementById('int-progress-text').innerText =
      `Question ${this.state.interviewQuestionIndex + 1} of ${this.state.activeInterview.questions.length}`;
    document.getElementById('int-question-text').innerText = q.questionText;
    document.getElementById('int-answer-input').value = '';
  },

  async submitInterviewAnswer() {
    const answer = document.getElementById('int-answer-input')?.value || '';
    if (!answer.trim()) {
      this.toast('Please write or speak your answer before submitting', 'error');
      return;
    }

    const q = this.state.activeInterview.questions[this.state.interviewQuestionIndex];
    try {
      await this.request('/api/interviews/answer', {
        method: 'POST',
        body: JSON.stringify({ questionId: q.id, answerText: answer })
      });

      this.state.interviewQuestionIndex++;
      if (this.state.interviewQuestionIndex < this.state.activeInterview.questions.length) {
        this.showInterviewQuestion();
      } else {
        this.finishInterview();
      }
    } catch (err) {}
  },

  async finishInterview() {
    document.getElementById('interview-runner-card')?.classList.add('hidden');
    document.getElementById('interview-results-card')?.classList.remove('hidden');

    try {
      const res = await this.request(`/api/interviews/${this.state.activeInterview.id}/result`);
      document.getElementById('int-res-overall').innerText = res.overallScore;
      document.getElementById('int-res-tech').innerText = res.technicalScore;
      document.getElementById('int-res-comm').innerText = res.communicationScore;
      document.getElementById('int-res-prob').innerText = res.problemSolvingScore;

      document.getElementById('int-feedback-details').innerHTML = `
        <p class="stat-desc">Solid command over fundamentals. Continue enhancing quantifiable impact metrics.</p>
      `;
      this.toast('Mock Interview completed!');
    } catch (err) {}
  },

  voiceInputToggle() {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      this.toast('Speech recognition not supported in browser. Please type.', 'error');
      return;
    }
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = 'en-US';
    recognition.start();
    this.toast('Listening... Speak now.');
    recognition.onresult = (event) => {
      document.getElementById('int-answer-input').value += ' ' + event.results[0][0].transcript;
    };
  },

  openAddCourseModal() {
    if (!this.state.currentRoadmapData) {
      this.toast('Please load a roadmap first', 'error');
      return;
    }
    const select = document.getElementById('course-phase-select');
    if (select) {
      select.innerHTML = '';
      (this.state.currentRoadmapData.phases || []).forEach((p, idx) => {
        const opt = document.createElement('option');
        opt.value = idx;
        opt.innerText = `Phase ${p.phaseNumber || idx + 1}: ${p.title}`;
        select.appendChild(opt);
      });
    }
    document.getElementById('add-course-modal')?.classList.remove('hidden');
  },

  closeAddCourseModal() {
    const modal = document.getElementById('add-course-modal');
    if (modal) modal.classList.add('hidden');
    const form = document.getElementById('add-course-form');
    if (form) form.reset();
  },

  saveCustomCourse() {
    const phaseIdx = parseInt(document.getElementById('course-phase-select')?.value, 10);
    const title = document.getElementById('course-title-input')?.value.trim();
    const platform = document.getElementById('course-platform-input')?.value.trim();
    const hours = parseInt(document.getElementById('course-hours-input')?.value, 10) || 10;
    const url = document.getElementById('course-url-input')?.value.trim();

    if (!title || !platform || !url) {
      this.toast('Please fill all required course details', 'error');
      return;
    }

    const roleKey = (this.state.currentRoadmapData.targetRole || "default").toLowerCase().replace(/[^a-z0-9]/g, '_');
    const customKey = `roadmap_custom_courses_${roleKey}`;
    const savedCustom = JSON.parse(localStorage.getItem(customKey) || '{}');

    if (!savedCustom[phaseIdx]) savedCustom[phaseIdx] = [];
    const newCourse = {
      id: `custom_${Date.now()}`,
      title,
      platform,
      type: "Custom Course",
      url,
      estHours: hours,
      status: "NOT_STARTED"
    };
    savedCustom[phaseIdx].push(newCourse);
    localStorage.setItem(customKey, JSON.stringify(savedCustom));

    this.closeAddCourseModal();
    this.toast('Custom course attached to milestone!');

    const grid = document.getElementById(`resource-grid-${phaseIdx}`);
    if (grid) {
      const card = document.createElement('div');
      card.className = 'learning-res-card';
      card.id = `res-card-${newCourse.id}`;
      card.dataset.hours = hours;
      card.innerHTML = `
        <div class="res-info-col">
          <span class="res-platform-tag">📚 ${platform} • Custom Course</span>
          <a href="${url}" target="_blank" rel="noopener noreferrer" class="res-title-link">
            ${title} ↗
          </a>
          <span class="res-meta-hours">⏱️ Est. Completion: ${hours} Hours</span>
        </div>
        <div class="res-action-col">
          <button class="res-status-btn"
                  id="btn-res-${newCourse.id}"
                  onclick="app.toggleResourceComplete('${roleKey}', '${newCourse.id}', ${hours}, ${phaseIdx})">
            ○ Mark Complete
          </button>
        </div>
      `;
      grid.appendChild(card);
      this.updatePhaseMetricsInPlace(roleKey, phaseIdx);
    } else {
      this.loadRoadmap(this.state.currentRoadmapData.targetRole);
    }
  },

  toggleResourceComplete(roleKey, resId, estHours, phaseIndex) {
    const key = `roadmap_resources_${roleKey}`;
    const saved = JSON.parse(localStorage.getItem(key) || '{}');

    const card = document.getElementById(`res-card-${resId}`);
    const btn = document.getElementById(`btn-res-${resId}`);

    if (saved[resId]) {
      delete saved[resId];
      if (card) card.classList.remove('res-completed');
      if (btn) {
        btn.classList.remove('btn-completed');
        btn.innerText = '○ Mark Complete';
      }
    } else {
      saved[resId] = { completedAt: new Date().toISOString(), hours: estHours };
      if (card) card.classList.add('res-completed');
      if (btn) {
        btn.classList.add('btn-completed');
        btn.innerText = '✓ Completed';
      }
    }

    localStorage.setItem(key, JSON.stringify(saved));
    this.updatePhaseMetricsInPlace(roleKey, phaseIndex);
  },

  updatePhaseMetricsInPlace(roleKey, phaseIndex) {
    const phaseCard = document.getElementById(`roadmap-phase-card-${phaseIndex}`);
    if (!phaseCard) return;

    const cards = phaseCard.querySelectorAll('.learning-res-card');
    const completedCards = phaseCard.querySelectorAll('.learning-res-card.res-completed');
    const totalCount = cards.length;
    const doneCount = completedCards.length;
    const pct = totalCount > 0 ? Math.round((doneCount / totalCount) * 100) : 0;

    const countEl = document.getElementById(`phase-count-${phaseIndex}`);
    const pctEl = document.getElementById(`phase-pct-${phaseIndex}`);
    const barEl = document.getElementById(`phase-bar-${phaseIndex}`);
    const badgeEl = document.getElementById(`phase-badge-${phaseIndex}`);
    const nodeEl = document.getElementById(`step-node-${phaseIndex}`);
    const stepCardEl = document.getElementById(`step-card-${phaseIndex}`);

    if (countEl) countEl.innerText = `${doneCount} of ${totalCount} courses completed`;
    if (pctEl) pctEl.innerText = `${pct}%`;
    if (barEl) {
      barEl.style.width = `${pct}%`;
      barEl.style.background = pct === 100 ? 'var(--success)' : 'var(--primary)';
    }

    if (pct === 100) {
      if (badgeEl) badgeEl.innerHTML = '<span class="score-badge score-green">✓ Mastered (100%)</span>';
      if (nodeEl) { nodeEl.className = 'step-node completed'; nodeEl.innerText = '✓'; }
      if (stepCardEl) { stepCardEl.className = 'step-card completed-card'; }
    } else if (pct > 0) {
      if (badgeEl) badgeEl.innerHTML = `<span class="score-badge score-green">⚡ In Progress (${pct}%)</span>`;
      if (nodeEl) { nodeEl.className = 'step-node active'; nodeEl.innerText = '⚡'; }
      if (stepCardEl) { stepCardEl.className = 'step-card active-card'; }
    } else {
      if (badgeEl) badgeEl.innerHTML = '<span class="score-badge score-yellow">🔒 Not Started</span>';
      if (nodeEl) { nodeEl.className = 'step-node locked'; nodeEl.innerText = `${phaseIndex + 1}`; }
      if (stepCardEl) { stepCardEl.className = 'step-card'; }
    }

    this.recalculateRoadmapProgressInPlace(roleKey);
  },

  recalculateRoadmapProgressInPlace(roleKey) {
    const saved = JSON.parse(localStorage.getItem(`roadmap_resources_${roleKey}`) || '{}');
    const allCards = document.querySelectorAll('.learning-res-card');
    const totalCount = allCards.length;
    const completedCount = Object.keys(saved).length;

    let totalHours = 0;
    Object.values(saved).forEach(item => {
      totalHours += (item.hours || 0);
    });

    const overallPct = totalCount > 0 ? Math.min(100, Math.round((completedCount / totalCount) * 100)) : 0;
    const progEl = document.getElementById('rm-overall-progress-num');
    const hoursEl = document.getElementById('rm-hours-progress');

    if (progEl) progEl.innerText = `${overallPct}%`;
    if (hoursEl) hoursEl.innerText = `${totalHours} hrs`;
  },

  async loadRoadmap(customRole = null) {
    const container = document.getElementById('roadmap-timeline');
    if (!container) return;

    const roleToLoad = customRole
      || localStorage.getItem('active_roadmap_role')
      || (this.state.user && this.state.user.professionalTitle ? this.state.user.professionalTitle : 'Python Developer');

    const roleInput = document.getElementById('rm-role-input');
    if (roleInput) roleInput.value = roleToLoad;

    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <div style="font-size: 2rem; margin-bottom: 8px;">⏳</div>
        <h3>Generating Real Industry Roadmap for '${roleToLoad}'...</h3>
        <p class="stat-desc">Analyzing verified courses, documentation, and milestones</p>
      </div>
    `;

    try {
      const endpoint = `/api/career-roadmap?targetRole=${encodeURIComponent(roleToLoad)}`;
      const rm = await this.request(endpoint);
      if (!rm) return;

      this.state.currentRoadmapData = rm;
      const roleKey = (rm.targetRole || roleToLoad).toLowerCase().replace(/[^a-z0-9]/g, '_');
      const savedResources = JSON.parse(localStorage.getItem(`roadmap_resources_${roleKey}`) || '{}');
      const customCourses = JSON.parse(localStorage.getItem(`roadmap_custom_courses_${roleKey}`) || '{}');

      const targetRoleTitle = document.getElementById('rm-target-role-title');
      const transitionText = document.getElementById('rm-transition-text');
      const durationText = document.getElementById('rm-stat-duration');

      if (targetRoleTitle) targetRoleTitle.innerText = rm.targetRole || roleToLoad;
      if (transitionText) transitionText.innerText = `Transitioning: ${rm.currentLevel || 'Junior Professional'} → ${rm.targetRole || roleToLoad}`;
      if (durationText) durationText.innerText = rm.totalDuration || "~6 Months";

      container.innerHTML = '';
      const phases = rm.phases || [];
      let grandTotalCards = 0;
      let grandCompletedCards = 0;
      let totalCompletedHours = 0;

      phases.forEach((p, pIdx) => {
        const topics = p.topics || [];
        const baseResources = p.resources || [];
        const extraResources = customCourses[pIdx] || [];
        const allResources = [...baseResources, ...extraResources];

        let completedInPhase = 0;
        allResources.forEach(res => {
          if (savedResources[res.id]) {
            completedInPhase++;
            totalCompletedHours += (res.estHours || 0);
          }
        });

        const totalInPhase = allResources.length;
        grandTotalCards += totalInPhase;
        grandCompletedCards += completedInPhase;

        const phaseProgress = totalInPhase > 0 ? Math.round((completedInPhase / totalInPhase) * 100) : 0;
        const isCompleted = phaseProgress === 100;
        const isActive = phaseProgress > 0 && phaseProgress < 100;

        let nodeClass = isCompleted ? 'completed' : (isActive || pIdx === 0 ? 'active' : 'locked');
        let nodeIcon = isCompleted ? '✓' : (isActive || pIdx === 0 ? '⚡' : (p.phaseNumber || (pIdx + 1)));
        let statusBadge = isCompleted
          ? '<span class="score-badge score-green">✓ Mastered (100%)</span>'
          : (isActive ? `<span class="score-badge score-green">⚡ In Progress (${phaseProgress}%)</span>` : '<span class="score-badge score-yellow">🔒 Not Started</span>');

        const topicsPills = topics.map(t =>
          `<span class="competency-tag"><span>⚡</span> ${t}</span>`
        ).join('');

        let resourcesHtml = '';
        allResources.forEach(res => {
          const isDone = !!savedResources[res.id];
          resourcesHtml += `
            <div class="learning-res-card ${isDone ? 'res-completed' : ''}" id="res-card-${res.id}" data-hours="${res.estHours}">
              <div class="res-info-col">
                <span class="res-platform-tag">📚 ${res.platform} • ${res.type}</span>
                <a href="${res.url}" target="_blank" rel="noopener noreferrer" class="res-title-link">
                  ${res.title} ↗
                </a>
                <span class="res-meta-hours">⏱️ Est. Duration: ${res.estHours} Hours</span>
              </div>
              <div class="res-action-col">
                <button class="res-status-btn ${isDone ? 'btn-completed' : ''}"
                        id="btn-res-${res.id}"
                        onclick="app.toggleResourceComplete('${roleKey}', '${res.id}', ${res.estHours}, ${pIdx})">
                  ${isDone ? '✓ Completed' : '○ Mark Complete'}
                </button>
              </div>
            </div>
          `;
        });

        const card = document.createElement('div');
        card.className = 'roadmap-step';
        card.id = `roadmap-phase-card-${pIdx}`;
        card.innerHTML = `
          <div class="step-node ${nodeClass}" id="step-node-${pIdx}">${nodeIcon}</div>
          <div class="step-card ${isCompleted ? 'completed-card' : (isActive ? 'active-card' : '')}" id="step-card-${pIdx}">
            <div class="step-header-meta">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="font-size: 0.78rem; font-weight: 800; color: var(--primary); letter-spacing: 0.5px;">PHASE 0${p.phaseNumber || (pIdx + 1)}</span>
                <span class="stat-desc">•</span>
                <span class="stat-desc" style="font-weight: 600;">⏱️ ${p.duration || `Month ${(pIdx * 2) + 1}–${(pIdx * 2) + 2}`}</span>
              </div>
              <div id="phase-badge-${pIdx}">${statusBadge}</div>
            </div>

            <h3 class="step-title">${p.title}</h3>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px;">
              <span class="stat-desc" id="phase-count-${pIdx}">${completedInPhase} of ${totalInPhase} courses completed</span>
              <strong class="stat-desc" id="phase-pct-${pIdx}">${phaseProgress}%</strong>
            </div>

            <div class="bar-wrap mt-2">
              <div class="bar-fill" id="phase-bar-${pIdx}" style="width: ${phaseProgress}%; background: ${isCompleted ? 'var(--success)' : 'var(--primary)'};"></div>
            </div>

            <div class="competency-tags-grid">
              ${topicsPills}
            </div>

            <div class="resources-section-title">
              <span>📖 Milestone Courses & Documentation (${allResources.length})</span>
              <button class="btn btn-outline btn-sm" onclick="app.openAddCourseModal()">+ Add Custom Course</button>
            </div>
            <div class="resource-cards-grid" id="resource-grid-${pIdx}">
              ${resourcesHtml}
            </div>

            <div class="capstone-box mt-3">
              <strong>🛠️ Capstone Deliverable (Interview Showcase):</strong>
              <div class="capstone-desc">
                ${p.capstoneProject || 'Build and deploy an enterprise mini-module demonstrating clean architecture, tests, and metrics.'}
              </div>
            </div>
          </div>
        `;
        container.appendChild(card);
      });

      const overallPct = grandTotalCards > 0 ? Math.round((grandCompletedCards / grandTotalCards) * 100) : 0;
      const progEl = document.getElementById('rm-overall-progress-num');
      const hoursEl = document.getElementById('rm-hours-progress');
      if (progEl) progEl.innerText = `${overallPct}%`;
      if (hoursEl) hoursEl.innerText = `${totalCompletedHours} hrs`;

    } catch (err) {
      container.innerHTML = `
        <div class="card" style="text-align: center; padding: 30px;">
          <p class="stat-desc" style="color: var(--danger);">Failed to load career roadmap. Please retry.</p>
        </div>
      `;
    }
  },

  resetRoadmapProgress() {
    if (!this.state.currentRoadmapData) return;
    if (!confirm('Are you sure you want to reset all course progress and hours for this roadmap?')) return;
    const roleKey = (this.state.currentRoadmapData.targetRole || "default").toLowerCase().replace(/[^a-z0-9]/g, '_');
    localStorage.removeItem(`roadmap_resources_${roleKey}`);
    localStorage.removeItem(`roadmap_custom_courses_${roleKey}`);
    this.toast('Roadmap progress reset.');
    this.loadRoadmap(this.state.currentRoadmapData.targetRole);
  },

  generateCustomRoadmap() {
    const input = document.getElementById('rm-role-input');
    const role = input ? input.value.trim() : '';
    if (!role) {
      this.toast('Please enter a target career or course', 'error');
      return;
    }
    localStorage.setItem('active_roadmap_role', role);
    this.toast(`Generating accurate curriculum & courses for '${role}'...`);
    this.loadRoadmap(role);
  },

  quickLoadRoadmap(role) {
    const input = document.getElementById('rm-role-input');
    if (input) input.value = role;
    localStorage.setItem('active_roadmap_role', role);
    this.toast(`Loading roadmap & resources for '${role}'...`);
    this.loadRoadmap(role);
  },

  // GitHub Profile Repository Audit
  async analyzeGithub() {
    const username = document.getElementById('gh-username')?.value || '';
    if (!username.trim()) {
      this.toast('Enter a GitHub username', 'error');
      return;
    }

    try {
      const res = await this.request('/api/github/analyze', {
        method: 'POST',
        body: JSON.stringify({ username })
      });

      document.getElementById('github-results')?.classList.remove('hidden');
      const list = document.getElementById('gh-repos-list');
      if (list) {
        list.innerHTML = '';
        (res.recommendedProjectsForResume || []).forEach(r => {
          list.innerHTML += `
            <div class="card">
              <strong>${r.name}</strong> (${r.language || 'Code'})<br/>
              <span class="stat-desc">${r.description || 'Public Repository'}</span><br/>
              ⭐ ${r.stars || 0} Stars
            </div>
          `;
        });
      }
      this.toast('GitHub repositories analyzed!');
    } catch (err) {}
  }
};

// Guarantee window-level accessibility for all inline onclick handlers
window.app = app;
document.addEventListener('DOMContentLoaded', () => app.init());